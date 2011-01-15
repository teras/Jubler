/*
 * RTSP/SDP client
 * Copyright (c) 2002 Fabrice Bellard.
 *
 * This file is part of FFmpeg.
 *
 * FFmpeg is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFmpeg; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

/* needed by inet_aton() */
#define _SVID_SOURCE

#include "libavutil/avstring.h"
#include "avformat.h"

#include <sys/time.h>
#ifdef HAVE_SYS_SELECT_H
#include <sys/select.h>
#endif
#include <strings.h>
#include "network.h"
#include "rtsp.h"

#include "rtp_internal.h"
#include "rdt.h"

//#define DEBUG
//#define DEBUG_RTP_TCP

enum RTSPClientState {
    RTSP_STATE_IDLE,
    RTSP_STATE_PLAYING,
    RTSP_STATE_PAUSED,
};

enum RTSPServerType {
    RTSP_SERVER_RTP,  /*< Standard-compliant RTP-server */
    RTSP_SERVER_REAL, /*< Realmedia-style server */
    RTSP_SERVER_LAST
};

enum RTSPTransport {
    RTSP_TRANSPORT_RTP,
    RTSP_TRANSPORT_RDT,
    RTSP_TRANSPORT_LAST
};

typedef struct RTSPState {
    URLContext *rtsp_hd; /* RTSP TCP connexion handle */
    int nb_rtsp_streams;
    struct RTSPStream **rtsp_streams;

    enum RTSPClientState state;
    int64_t seek_timestamp;

    /* XXX: currently we use unbuffered input */
    //    ByteIOContext rtsp_gb;
    int seq;        /* RTSP command sequence number */
    char session_id[512];
    enum RTSPTransport transport;
    enum RTSPLowerTransport lower_transport;
    enum RTSPServerType server_type;
    char last_reply[2048]; /* XXX: allocate ? */
    void *cur_tx;
    int need_subscription;
} RTSPState;

typedef struct RTSPStream {
    URLContext *rtp_handle; /* RTP stream handle */
    void *tx_ctx; /* RTP/RDT parse context */

    int stream_index; /* corresponding stream index, if any. -1 if none (MPEG2TS case) */
    int interleaved_min, interleaved_max;  /* interleave ids, if TCP transport */
    char control_url[1024]; /* url for this stream (from SDP) */

    int sdp_port; /* port (from SDP content - not used in RTSP) */
    struct in_addr sdp_ip; /* IP address  (from SDP content - not used in RTSP) */
    int sdp_ttl;  /* IP TTL (from SDP content - not used in RTSP) */
    int sdp_payload_type; /* payload type - only used in SDP */
    rtp_payload_data_t rtp_payload_data; /* rtp payload parsing infos from SDP */

    RTPDynamicProtocolHandler *dynamic_handler; ///< Only valid if it's a dynamic protocol. (This is the handler structure)
    PayloadContext *dynamic_protocol_context; ///< Only valid if it's a dynamic protocol. (This is any private data associated with the dynamic protocol)
} RTSPStream;

static int rtsp_read_play(AVFormatContext *s);

/* XXX: currently, the only way to change the protocols consists in
   changing this variable */

#if LIBAVFORMAT_VERSION_INT < (53 << 16)
int rtsp_default_protocols = (1 << RTSP_LOWER_TRANSPORT_UDP);
#endif

static int rtsp_probe(AVProbeData *p)
{
    if (av_strstart(p->filename, "rtsp:", NULL))
        return AVPROBE_SCORE_MAX;
    return 0;
}

static int redir_isspace(int c)
{
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
}

static void skip_spaces(const char **pp)
{
    const char *p;
    p = *pp;
    while (redir_isspace(*p))
        p++;
    *pp = p;
}

static void get_word_sep(char *buf, int buf_size, const char *sep,
                         const char **pp)
{
    const char *p;
    char *q;

    p = *pp;
    if (*p == '/')
        p++;
    skip_spaces(&p);
    q = buf;
    while (!strchr(sep, *p) && *p != '\0') {
        if ((q - buf) < buf_size - 1)
            *q++ = *p;
        p++;
    }
    if (buf_size > 0)
        *q = '\0';
    *pp = p;
}

static void get_word(char *buf, int buf_size, const char **pp)
{
    const char *p;
    char *q;

    p = *pp;
    skip_spaces(&p);
    q = buf;
    while (!redir_isspace(*p) && *p != '\0') {
        if ((q - buf) < buf_size - 1)
            *q++ = *p;
        p++;
    }
    if (buf_size > 0)
        *q = '\0';
    *pp = p;
}

/* parse the rtpmap description: <codec_name>/<clock_rate>[/<other
   params>] */
static int sdp_parse_rtpmap(AVCodecContext *codec, RTSPStream *rtsp_st, int payload_type, const char *p)
{
    char buf[256];
    int i;
    AVCodec *c;
    const char *c_name;

    /* Loop into AVRtpDynamicPayloadTypes[] and AVRtpPayloadTypes[] and
       see if we can handle this kind of payload */
    get_word_sep(buf, sizeof(buf), "/", &p);
    if (payload_type >= RTP_PT_PRIVATE) {
        RTPDynamicProtocolHandler *handler= RTPFirstDynamicPayloadHandler;
        while(handler) {
            if (!strcmp(buf, handler->enc_name) && (codec->codec_type == handler->codec_type)) {
                codec->codec_id = handler->codec_id;
                rtsp_st->dynamic_handler= handler;
                if(handler->open) {
                    rtsp_st->dynamic_protocol_context= handler->open();
                }
                break;
            }
            handler= handler->next;
        }
    } else {
        /* We are in a standard case ( from http://www.iana.org/assignments/rtp-parameters) */
        /* search into AVRtpPayloadTypes[] */
        codec->codec_id = ff_rtp_codec_id(buf, codec->codec_type);
    }

    c = avcodec_find_decoder(codec->codec_id);
    if (c && c->name)
        c_name = c->name;
    else
        c_name = (char *)NULL;

    if (c_name) {
        get_word_sep(buf, sizeof(buf), "/", &p);
        i = atoi(buf);
        switch (codec->codec_type) {
            case CODEC_TYPE_AUDIO:
                av_log(codec, AV_LOG_DEBUG, " audio codec set to : %s\n", c_name);
                codec->sample_rate = RTSP_DEFAULT_AUDIO_SAMPLERATE;
                codec->channels = RTSP_DEFAULT_NB_AUDIO_CHANNELS;
                if (i > 0) {
                    codec->sample_rate = i;
                    get_word_sep(buf, sizeof(buf), "/", &p);
                    i = atoi(buf);
                    if (i > 0)
                        codec->channels = i;
                    // TODO: there is a bug here; if it is a mono stream, and less than 22000Hz, faad upconverts to stereo and twice the
                    //  frequency.  No problem, but the sample rate is being set here by the sdp line.  Upcoming patch forthcoming. (rdm)
                }
                av_log(codec, AV_LOG_DEBUG, " audio samplerate set to : %i\n", codec->sample_rate);
                av_log(codec, AV_LOG_DEBUG, " audio channels set to : %i\n", codec->channels);
                break;
            case CODEC_TYPE_VIDEO:
                av_log(codec, AV_LOG_DEBUG, " video codec set to : %s\n", c_name);
                break;
            default:
                break;
        }
        return 0;
    }

    return -1;
}

/* return the length and optionnaly the data */
static int hex_to_data(uint8_t *data, const char *p)
{
    int c, len, v;

    len = 0;
    v = 1;
    for(;;) {
        skip_spaces(&p);
        if (p == '\0')
            break;
        c = toupper((unsigned char)*p++);
        if (c >= '0' && c <= '9')
            c = c - '0';
        else if (c >= 'A' && c <= 'F')
            c = c - 'A' + 10;
        else
            break;
        v = (v << 4) | c;
        if (v & 0x100) {
            if (data)
                data[len] = v;
            len++;
            v = 1;
        }
    }
    return len;
}

static void sdp_parse_fmtp_config(AVCodecContext *codec, char *attr, char *value)
{
    switch (codec->codec_id) {
        case CODEC_ID_MPEG4:
        case CODEC_ID_AAC:
            if (!strcmp(attr, "config")) {
                /* decode the hexa encoded parameter */
                int len = hex_to_data(NULL, value);
                codec->extradata = av_mallocz(len + FF_INPUT_BUFFER_PADDING_SIZE);
                if (!codec->extradata)
                    return;
                codec->extradata_size = len;
                hex_to_data(codec->extradata, value);
            }
            break;
        default:
            break;
    }
    return;
}

typedef struct attrname_map
{
    const char *str;
    uint16_t type;
    uint32_t offset;
} attrname_map_t;

/* All known fmtp parmeters and the corresping RTPAttrTypeEnum */
#define ATTR_NAME_TYPE_INT 0
#define ATTR_NAME_TYPE_STR 1
static const attrname_map_t attr_names[]=
{
    {"SizeLength",       ATTR_NAME_TYPE_INT, offsetof(rtp_payload_data_t, sizelength)},
    {"IndexLength",      ATTR_NAME_TYPE_INT, offsetof(rtp_payload_data_t, indexlength)},
    {"IndexDeltaLength", ATTR_NAME_TYPE_INT, offsetof(rtp_payload_data_t, indexdeltalength)},
    {"profile-level-id", ATTR_NAME_TYPE_INT, offsetof(rtp_payload_data_t, profile_level_id)},
    {"StreamType",       ATTR_NAME_TYPE_INT, offsetof(rtp_payload_data_t, streamtype)},
    {"mode",             ATTR_NAME_TYPE_STR, offsetof(rtp_payload_data_t, mode)},
    {NULL, -1, -1},
};

/** parse the attribute line from the fmtp a line of an sdp resonse.  This is broken out as a function
* because it is used in rtp_h264.c, which is forthcoming.
*/
int rtsp_next_attr_and_value(const char **p, char *attr, int attr_size, char *value, int value_size)
{
    skip_spaces(p);
    if(**p)
    {
        get_word_sep(attr, attr_size, "=", p);
        if (**p == '=')
            (*p)++;
        get_word_sep(value, value_size, ";", p);
        if (**p == ';')
            (*p)++;
        return 1;
    }
    return 0;
}

/* parse a SDP line and save stream attributes */
static void sdp_parse_fmtp(AVStream *st, const char *p)
{
    char attr[256];
    char value[4096];
    int i;

    RTSPStream *rtsp_st = st->priv_data;
    AVCodecContext *codec = st->codec;
    rtp_payload_data_t *rtp_payload_data = &rtsp_st->rtp_payload_data;

    /* loop on each attribute */
    while(rtsp_next_attr_and_value(&p, attr, sizeof(attr), value, sizeof(value)))
    {
        /* grab the codec extra_data from the config parameter of the fmtp line */
        sdp_parse_fmtp_config(codec, attr, value);
        /* Looking for a known attribute */
        for (i = 0; attr_names[i].str; ++i) {
            if (!strcasecmp(attr, attr_names[i].str)) {
                if (attr_names[i].type == ATTR_NAME_TYPE_INT)
                    *(int *)((char *)rtp_payload_data + attr_names[i].offset) = atoi(value);
                else if (attr_names[i].type == ATTR_NAME_TYPE_STR)
                    *(char **)((char *)rtp_payload_data + attr_names[i].offset) = av_strdup(value);
            }
        }
    }
}

/** Parse a string \p in the form of Range:npt=xx-xx, and determine the start
 *  and end time.
 *  Used for seeking in the rtp stream.
 */
static void rtsp_parse_range_npt(const char *p, int64_t *start, int64_t *end)
{
    char buf[256];

    skip_spaces(&p);
    if (!av_stristart(p, "npt=", &p))
        return;

    *start = AV_NOPTS_VALUE;
    *end = AV_NOPTS_VALUE;

    get_word_sep(buf, sizeof(buf), "-", &p);
    *start = parse_date(buf, 1);
    if (*p == '-') {
        p++;
        get_word_sep(buf, sizeof(buf), "-", &p);
        *end = parse_date(buf, 1);
    }
//    av_log(NULL, AV_LOG_DEBUG, "Range Start: %lld\n", *start);
//    av_log(NULL, AV_LOG_DEBUG, "Range End: %lld\n", *end);
}

typedef struct SDPParseState {
    /* SDP only */
    struct in_addr default_ip;
    int default_ttl;
} SDPParseState;

static void sdp_parse_line(AVFormatContext *s, SDPParseState *s1,
                           int letter, const char *buf)
{
    RTSPState *rt = s->priv_data;
    char buf1[64], st_type[64];
    const char *p;
    enum CodecType codec_type;
    int payload_type, i;
    AVStream *st;
    RTSPStream *rtsp_st;
    struct in_addr sdp_ip;
    int ttl;

#ifdef DEBUG
    printf("sdp: %c='%s'\n", letter, buf);
#endif

    p = buf;
    switch(letter) {
    case 'c':
        get_word(buf1, sizeof(buf1), &p);
        if (strcmp(buf1, "IN") != 0)
            return;
        get_word(buf1, sizeof(buf1), &p);
        if (strcmp(buf1, "IP4") != 0)
            return;
        get_word_sep(buf1, sizeof(buf1), "/", &p);
        if (inet_aton(buf1, &sdp_ip) == 0)
            return;
        ttl = 16;
        if (*p == '/') {
            p++;
            get_word_sep(buf1, sizeof(buf1), "/", &p);
            ttl = atoi(buf1);
        }
        if (s->nb_streams == 0) {
            s1->default_ip = sdp_ip;
            s1->default_ttl = ttl;
        } else {
            st = s->streams[s->nb_streams - 1];
            rtsp_st = st->priv_data;
            rtsp_st->sdp_ip = sdp_ip;
            rtsp_st->sdp_ttl = ttl;
        }
        break;
    case 's':
        av_strlcpy(s->title, p, sizeof(s->title));
        break;
    case 'i':
        if (s->nb_streams == 0) {
            av_strlcpy(s->comment, p, sizeof(s->comment));
            break;
        }
        break;
    case 'm':
        /* new stream */
        get_word(st_type, sizeof(st_type), &p);
        if (!strcmp(st_type, "audio")) {
            codec_type = CODEC_TYPE_AUDIO;
        } else if (!strcmp(st_type, "video")) {
            codec_type = CODEC_TYPE_VIDEO;
        } else {
            return;
        }
        rtsp_st = av_mallocz(sizeof(RTSPStream));
        if (!rtsp_st)
            return;
        rtsp_st->stream_index = -1;
        dynarray_add(&rt->rtsp_streams, &rt->nb_rtsp_streams, rtsp_st);

        rtsp_st->sdp_ip = s1->default_ip;
        rtsp_st->sdp_ttl = s1->default_ttl;

        get_word(buf1, sizeof(buf1), &p); /* port */
        rtsp_st->sdp_port = atoi(buf1);

        get_word(buf1, sizeof(buf1), &p); /* protocol (ignored) */

        /* XXX: handle list of formats */
        get_word(buf1, sizeof(buf1), &p); /* format list */
        rtsp_st->sdp_payload_type = atoi(buf1);

        if (!strcmp(ff_rtp_enc_name(rtsp_st->sdp_payload_type), "MP2T")) {
            /* no corresponding stream */
        } else {
            st = av_new_stream(s, 0);
            if (!st)
                return;
            st->priv_data = rtsp_st;
            rtsp_st->stream_index = st->index;
            st->codec->codec_type = codec_type;
            if (rtsp_st->sdp_payload_type < RTP_PT_PRIVATE) {
                /* if standard payload type, we can find the codec right now */
                rtp_get_codec_info(st->codec, rtsp_st->sdp_payload_type);
            }
        }
        /* put a default control url */
        av_strlcpy(rtsp_st->control_url, s->filename, sizeof(rtsp_st->control_url));
        break;
    case 'a':
        if (av_strstart(p, "control:", &p) && s->nb_streams > 0) {
            char proto[32];
            /* get the control url */
            st = s->streams[s->nb_streams - 1];
            rtsp_st = st->priv_data;

            /* XXX: may need to add full url resolution */
            url_split(proto, sizeof(proto), NULL, 0, NULL, 0, NULL, NULL, 0, p);
            if (proto[0] == '\0') {
                /* relative control URL */
                av_strlcat(rtsp_st->control_url, "/", sizeof(rtsp_st->control_url));
                av_strlcat(rtsp_st->control_url, p,   sizeof(rtsp_st->control_url));
            } else {
                av_strlcpy(rtsp_st->control_url, p,   sizeof(rtsp_st->control_url));
            }
        } else if (av_strstart(p, "rtpmap:", &p)) {
            /* NOTE: rtpmap is only supported AFTER the 'm=' tag */
            get_word(buf1, sizeof(buf1), &p);
            payload_type = atoi(buf1);
            for(i = 0; i < s->nb_streams;i++) {
                st = s->streams[i];
                rtsp_st = st->priv_data;
                if (rtsp_st->sdp_payload_type == payload_type) {
                    sdp_parse_rtpmap(st->codec, rtsp_st, payload_type, p);
                }
            }
        } else if (av_strstart(p, "fmtp:", &p)) {
            /* NOTE: fmtp is only supported AFTER the 'a=rtpmap:xxx' tag */
            get_word(buf1, sizeof(buf1), &p);
            payload_type = atoi(buf1);
            for(i = 0; i < s->nb_streams;i++) {
                st = s->streams[i];
                rtsp_st = st->priv_data;
                if (rtsp_st->sdp_payload_type == payload_type) {
                    if(rtsp_st->dynamic_handler && rtsp_st->dynamic_handler->parse_sdp_a_line) {
                        if(!rtsp_st->dynamic_handler->parse_sdp_a_line(s, i, rtsp_st->dynamic_protocol_context, buf)) {
                            sdp_parse_fmtp(st, p);
                        }
                    } else {
                        sdp_parse_fmtp(st, p);
                    }
                }
            }
        } else if(av_strstart(p, "framesize:", &p)) {
            // let dynamic protocol handlers have a stab at the line.
            get_word(buf1, sizeof(buf1), &p);
            payload_type = atoi(buf1);
            for(i = 0; i < s->nb_streams;i++) {
                st = s->streams[i];
                rtsp_st = st->priv_data;
                if (rtsp_st->sdp_payload_type == payload_type) {
                    if(rtsp_st->dynamic_handler && rtsp_st->dynamic_handler->parse_sdp_a_line) {
                        rtsp_st->dynamic_handler->parse_sdp_a_line(s, i, rtsp_st->dynamic_protocol_context, buf);
                    }
                }
            }
        } else if(av_strstart(p, "range:", &p)) {
            int64_t start, end;

            // this is so that seeking on a streamed file can work.
            rtsp_parse_range_npt(p, &start, &end);
            s->start_time= start;
            s->duration= (end==AV_NOPTS_VALUE)?AV_NOPTS_VALUE:end-start; // AV_NOPTS_VALUE means live broadcast (and can't seek)
        } else if (av_strstart(p, "IsRealDataType:integer;",&p)) {
            if (atoi(p) == 1)
                rt->transport = RTSP_TRANSPORT_RDT;
        } else if (s->nb_streams > 0) {
            rtsp_st = s->streams[s->nb_streams - 1]->priv_data;
            if (rtsp_st->dynamic_handler &&
                rtsp_st->dynamic_handler->parse_sdp_a_line)
                rtsp_st->dynamic_handler->parse_sdp_a_line(s, s->nb_streams - 1,
                    rtsp_st->dynamic_protocol_context, buf);
        }
        break;
    }
}

static int sdp_parse(AVFormatContext *s, const char *content)
{
    const char *p;
    int letter;
    char buf[2048], *q;
    SDPParseState sdp_parse_state, *s1 = &sdp_parse_state;

    memset(s1, 0, sizeof(SDPParseState));
    p = content;
    for(;;) {
        skip_spaces(&p);
        letter = *p;
        if (letter == '\0')
            break;
        p++;
        if (*p != '=')
            goto next_line;
        p++;
        /* get the content */
        q = buf;
        while (*p != '\n' && *p != '\r' && *p != '\0') {
            if ((q - buf) < sizeof(buf) - 1)
                *q++ = *p;
            p++;
        }
        *q = '\0';
        sdp_parse_line(s, s1, letter, buf);
    next_line:
        while (*p != '\n' && *p != '\0')
            p++;
        if (*p == '\n')
            p++;
    }
    return 0;
}

static void rtsp_parse_range(int *min_ptr, int *max_ptr, const char **pp)
{
    const char *p;
    int v;

    p = *pp;
    skip_spaces(&p);
    v = strtol(p, (char **)&p, 10);
    if (*p == '-') {
        p++;
        *min_ptr = v;
        v = strtol(p, (char **)&p, 10);
        *max_ptr = v;
    } else {
        *min_ptr = v;
        *max_ptr = v;
    }
    *pp = p;
}

/* XXX: only one transport specification is parsed */
static void rtsp_parse_transport(RTSPHeader *reply, const char *p)
{
    char transport_protocol[16];
    char profile[16];
    char lower_transport[16];
    char parameter[16];
    RTSPTransportField *th;
    char buf[256];

    reply->nb_transports = 0;

    for(;;) {
        skip_spaces(&p);
        if (*p == '\0')
            break;

        th = &reply->transports[reply->nb_transports];

        get_word_sep(transport_protocol, sizeof(transport_protocol),
                     "/", &p);
        if (*p == '/')
            p++;
        if (!strcasecmp (transport_protocol, "rtp")) {
            get_word_sep(profile, sizeof(profile), "/;,", &p);
            lower_transport[0] = '\0';
            /* rtp/avp/<protocol> */
            if (*p == '/') {
                p++;
                get_word_sep(lower_transport, sizeof(lower_transport),
                             ";,", &p);
            }
            th->transport = RTSP_TRANSPORT_RTP;
        } else if (!strcasecmp (transport_protocol, "x-pn-tng") ||
                   !strcasecmp (transport_protocol, "x-real-rdt")) {
            /* x-pn-tng/<protocol> */
            get_word_sep(lower_transport, sizeof(lower_transport), "/;,", &p);
            profile[0] = '\0';
            th->transport = RTSP_TRANSPORT_RDT;
        }
        if (!strcasecmp(lower_transport, "TCP"))
            th->lower_transport = RTSP_LOWER_TRANSPORT_TCP;
        else
            th->lower_transport = RTSP_LOWER_TRANSPORT_UDP;

        if (*p == ';')
            p++;
        /* get each parameter */
        while (*p != '\0' && *p != ',') {
            get_word_sep(parameter, sizeof(parameter), "=;,", &p);
            if (!strcmp(parameter, "port")) {
                if (*p == '=') {
                    p++;
                    rtsp_parse_range(&th->port_min, &th->port_max, &p);
                }
            } else if (!strcmp(parameter, "client_port")) {
                if (*p == '=') {
                    p++;
                    rtsp_parse_range(&th->client_port_min,
                                     &th->client_port_max, &p);
                }
            } else if (!strcmp(parameter, "server_port")) {
                if (*p == '=') {
                    p++;
                    rtsp_parse_range(&th->server_port_min,
                                     &th->server_port_max, &p);
                }
            } else if (!strcmp(parameter, "interleaved")) {
                if (*p == '=') {
                    p++;
                    rtsp_parse_range(&th->interleaved_min,
                                     &th->interleaved_max, &p);
                }
            } else if (!strcmp(parameter, "multicast")) {
                if (th->lower_transport == RTSP_LOWER_TRANSPORT_UDP)
                    th->lower_transport = RTSP_LOWER_TRANSPORT_UDP_MULTICAST;
            } else if (!strcmp(parameter, "ttl")) {
                if (*p == '=') {
                    p++;
                    th->ttl = strtol(p, (char **)&p, 10);
                }
            } else if (!strcmp(parameter, "destination")) {
                struct in_addr ipaddr;

                if (*p == '=') {
                    p++;
                    get_word_sep(buf, sizeof(buf), ";,", &p);
                    if (inet_aton(buf, &ipaddr))
                        th->destination = ntohl(ipaddr.s_addr);
                }
            }
            while (*p != ';' && *p != '\0' && *p != ',')
                p++;
            if (*p == ';')
                p++;
        }
        if (*p == ',')
            p++;

        reply->nb_transports++;
    }
}

void rtsp_parse_line(RTSPHeader *reply, const char *buf)
{
    const char *p;

    /* NOTE: we do case independent match for broken servers */
    p = buf;
    if (av_stristart(p, "Session:", &p)) {
        get_word_sep(reply->session_id, sizeof(reply->session_id), ";", &p);
    } else if (av_stristart(p, "Content-Length:", &p)) {
        reply->content_length = strtol(p, NULL, 10);
    } else if (av_stristart(p, "Transport:", &p)) {
        rtsp_parse_transport(reply, p);
    } else if (av_stristart(p, "CSeq:", &p)) {
        reply->seq = strtol(p, NULL, 10);
    } else if (av_stristart(p, "Range:", &p)) {
        rtsp_parse_range_npt(p, &reply->range_start, &reply->range_end);
    } else if (av_stristart(p, "RealChallenge1:", &p)) {
        skip_spaces(&p);
        av_strlcpy(reply->real_challenge, p, sizeof(reply->real_challenge));
    }
}

static int url_readbuf(URLContext *h, unsigned char *buf, int size)
{
    int ret, len;

    len = 0;
    while (len < size) {
        ret = url_read(h, buf+len, size-len);
        if (ret < 1)
            return ret;
        len += ret;
    }
    return len;
}

/* skip a RTP/TCP interleaved packet */
static void rtsp_skip_packet(AVFormatContext *s)
{
    RTSPState *rt = s->priv_data;
    int ret, len, len1;
    uint8_t buf[1024];

    ret = url_readbuf(rt->rtsp_hd, buf, 3);
    if (ret != 3)
        return;
    len = AV_RB16(buf + 1);
#ifdef DEBUG
    printf("skipping RTP packet len=%d\n", len);
#endif
    /* skip payload */
    while (len > 0) {
        len1 = len;
        if (len1 > sizeof(buf))
            len1 = sizeof(buf);
        ret = url_readbuf(rt->rtsp_hd, buf, len1);
        if (ret != len1)
            return;
        len -= len1;
    }
}

static void rtsp_send_cmd(AVFormatContext *s,
                          const char *cmd, RTSPHeader *reply,
                          unsigned char **content_ptr)
{
    RTSPState *rt = s->priv_data;
    char buf[4096], buf1[1024], *q;
    unsigned char ch;
    const char *p;
    int content_length, line_count;
    unsigned char *content = NULL;

    memset(reply, 0, sizeof(RTSPHeader));

    rt->seq++;
    av_strlcpy(buf, cmd, sizeof(buf));
    snprintf(buf1, sizeof(buf1), "CSeq: %d\r\n", rt->seq);
    av_strlcat(buf, buf1, sizeof(buf));
    if (rt->session_id[0] != '\0' && !strstr(cmd, "\nIf-Match:")) {
        snprintf(buf1, sizeof(buf1), "Session: %s\r\n", rt->session_id);
        av_strlcat(buf, buf1, sizeof(buf));
    }
    av_strlcat(buf, "\r\n", sizeof(buf));
#ifdef DEBUG
    printf("Sending:\n%s--\n", buf);
#endif
    url_write(rt->rtsp_hd, buf, strlen(buf));

    /* parse reply (XXX: use buffers) */
    line_count = 0;
    rt->last_reply[0] = '\0';
    for(;;) {
        q = buf;
        for(;;) {
            if (url_readbuf(rt->rtsp_hd, &ch, 1) != 1)
                break;
            if (ch == '\n')
                break;
            if (ch == '$') {
                /* XXX: only parse it if first char on line ? */
                rtsp_skip_packet(s);
            } else if (ch != '\r') {
                if ((q - buf) < sizeof(buf) - 1)
                    *q++ = ch;
            }
        }
        *q = '\0';
#ifdef DEBUG
        printf("line='%s'\n", buf);
#endif
        /* test if last line */
        if (buf[0] == '\0')
            break;
        p = buf;
        if (line_count == 0) {
            /* get reply code */
            get_word(buf1, sizeof(buf1), &p);
            get_word(buf1, sizeof(buf1), &p);
            reply->status_code = atoi(buf1);
        } else {
            rtsp_parse_line(reply, p);
            av_strlcat(rt->last_reply, p,    sizeof(rt->last_reply));
            av_strlcat(rt->last_reply, "\n", sizeof(rt->last_reply));
        }
        line_count++;
    }

    if (rt->session_id[0] == '\0' && reply->session_id[0] != '\0')
        av_strlcpy(rt->session_id, reply->session_id, sizeof(rt->session_id));

    content_length = reply->content_length;
    if (content_length > 0) {
        /* leave some room for a trailing '\0' (useful for simple parsing) */
        content = av_malloc(content_length + 1);
        (void)url_readbuf(rt->rtsp_hd, content, content_length);
        content[content_length] = '\0';
    }
    if (content_ptr)
        *content_ptr = content;
    else
        av_free(content);
}


/* close and free RTSP streams */
static void rtsp_close_streams(RTSPState *rt)
{
    int i;
    RTSPStream *rtsp_st;

    for(i=0;i<rt->nb_rtsp_streams;i++) {
        rtsp_st = rt->rtsp_streams[i];
        if (rtsp_st) {
            if (rtsp_st->tx_ctx) {
                if (rt->transport == RTSP_TRANSPORT_RDT)
                    ff_rdt_parse_close(rtsp_st->tx_ctx);
                else
                    rtp_parse_close(rtsp_st->tx_ctx);
            }
            if (rtsp_st->rtp_handle)
                url_close(rtsp_st->rtp_handle);
            if (rtsp_st->dynamic_handler && rtsp_st->dynamic_protocol_context)
                rtsp_st->dynamic_handler->close(rtsp_st->dynamic_protocol_context);
        }
    }
    av_free(rt->rtsp_streams);
}

static int
rtsp_open_transport_ctx(AVFormatContext *s, RTSPStream *rtsp_st)
{
    RTSPState *rt = s->priv_data;
    AVStream *st = NULL;

    /* open the RTP context */
    if (rtsp_st->stream_index >= 0)
        st = s->streams[rtsp_st->stream_index];
    if (!st)
        s->ctx_flags |= AVFMTCTX_NOHEADER;

    if (rt->transport == RTSP_TRANSPORT_RDT)
        rtsp_st->tx_ctx = ff_rdt_parse_open(s, st->index,
                                            rtsp_st->dynamic_protocol_context,
                                            rtsp_st->dynamic_handler);
    else
        rtsp_st->tx_ctx = rtp_parse_open(s, st, rtsp_st->rtp_handle,
                                         rtsp_st->sdp_payload_type,
                                         &rtsp_st->rtp_payload_data);

    if (!rtsp_st->tx_ctx) {
         return AVERROR(ENOMEM);
    } else if (rt->transport != RTSP_TRANSPORT_RDT) {
        if(rtsp_st->dynamic_handler) {
            rtp_parse_set_dynamic_protocol(rtsp_st->tx_ctx,
                                           rtsp_st->dynamic_protocol_context,
                                           rtsp_st->dynamic_handler);
        }
    }

    return 0;
}

/**
 * @returns 0 on success, <0 on error, 1 if protocol is unavailable.
 */
static int
make_setup_request (AVFormatContext *s, const char *host, int port,
                    int lower_transport, const char *real_challenge)
{
    RTSPState *rt = s->priv_data;
    int j, i, err;
    RTSPStream *rtsp_st;
    RTSPHeader reply1, *reply = &reply1;
    char cmd[2048];
    const char *trans_pref;

    if (rt->transport == RTSP_TRANSPORT_RDT)
        trans_pref = "x-pn-tng";
    else
        trans_pref = "RTP/AVP";

    /* for each stream, make the setup request */
    /* XXX: we assume the same server is used for the control of each
       RTSP stream */

    for(j = RTSP_RTP_PORT_MIN, i = 0; i < rt->nb_rtsp_streams; ++i) {
        char transport[2048];

        rtsp_st = rt->rtsp_streams[i];

        /* RTP/UDP */
        if (lower_transport == RTSP_LOWER_TRANSPORT_UDP) {
            char buf[256];

            /* first try in specified port range */
            if (RTSP_RTP_PORT_MIN != 0) {
                while(j <= RTSP_RTP_PORT_MAX) {
                    snprintf(buf, sizeof(buf), "rtp://%s?localport=%d", host, j);
                    j += 2; /* we will use two port by rtp stream (rtp and rtcp) */
                    if (url_open(&rtsp_st->rtp_handle, buf, URL_RDWR) == 0) {
                        goto rtp_opened;
                    }
                }
            }

/*            then try on any port
**            if (url_open(&rtsp_st->rtp_handle, "rtp://", URL_RDONLY) < 0) {
**                err = AVERROR_INVALIDDATA;
**                goto fail;
**            }
*/

        rtp_opened:
            port = rtp_get_local_port(rtsp_st->rtp_handle);
            snprintf(transport, sizeof(transport) - 1,
                     "%s/UDP;", trans_pref);
            if (rt->server_type != RTSP_SERVER_REAL)
                av_strlcat(transport, "unicast;", sizeof(transport));
            av_strlcatf(transport, sizeof(transport),
                     "client_port=%d", port);
            if (rt->transport == RTSP_TRANSPORT_RTP)
                av_strlcatf(transport, sizeof(transport), "-%d", port + 1);
        }

        /* RTP/TCP */
        else if (lower_transport == RTSP_LOWER_TRANSPORT_TCP) {
            snprintf(transport, sizeof(transport) - 1,
                     "%s/TCP", trans_pref);
        }

        else if (lower_transport == RTSP_LOWER_TRANSPORT_UDP_MULTICAST) {
            snprintf(transport, sizeof(transport) - 1,
                     "%s/UDP;multicast", trans_pref);
        }
        if (rt->server_type == RTSP_SERVER_REAL)
            av_strlcat(transport, ";mode=play", sizeof(transport));
        snprintf(cmd, sizeof(cmd),
                 "SETUP %s RTSP/1.0\r\n"
                 "Transport: %s\r\n",
                 rtsp_st->control_url, transport);
        if (i == 0 && rt->server_type == RTSP_SERVER_REAL) {
            char real_res[41], real_csum[9];
            ff_rdt_calc_response_and_checksum(real_res, real_csum,
                                              real_challenge);
            av_strlcatf(cmd, sizeof(cmd),
                        "If-Match: %s\r\n"
                        "RealChallenge2: %s, sd=%s\r\n",
                        rt->session_id, real_res, real_csum);
        }
        rtsp_send_cmd(s, cmd, reply, NULL);
        if (reply->status_code == 461 /* Unsupported protocol */ && i == 0) {
            err = 1;
            goto fail;
        } else if (reply->status_code != RTSP_STATUS_OK ||
                   reply->nb_transports != 1) {
            err = AVERROR_INVALIDDATA;
            goto fail;
        }

        /* XXX: same protocol for all streams is required */
        if (i > 0) {
            if (reply->transports[0].lower_transport != rt->lower_transport ||
                reply->transports[0].transport != rt->transport) {
                err = AVERROR_INVALIDDATA;
                goto fail;
            }
        } else {
            rt->lower_transport = reply->transports[0].lower_transport;
            rt->transport = reply->transports[0].transport;
        }

        /* close RTP connection if not choosen */
        if (reply->transports[0].lower_transport != RTSP_LOWER_TRANSPORT_UDP &&
            (lower_transport == RTSP_LOWER_TRANSPORT_UDP)) {
            url_close(rtsp_st->rtp_handle);
            rtsp_st->rtp_handle = NULL;
        }

        switch(reply->transports[0].lower_transport) {
        case RTSP_LOWER_TRANSPORT_TCP:
            rtsp_st->interleaved_min = reply->transports[0].interleaved_min;
            rtsp_st->interleaved_max = reply->transports[0].interleaved_max;
            break;

        case RTSP_LOWER_TRANSPORT_UDP:
            {
                char url[1024];

                /* XXX: also use address if specified */
                snprintf(url, sizeof(url), "rtp://%s:%d",
                         host, reply->transports[0].server_port_min);
                if (rtp_set_remote_url(rtsp_st->rtp_handle, url) < 0) {
                    err = AVERROR_INVALIDDATA;
                    goto fail;
                }
            }
            break;
        case RTSP_LOWER_TRANSPORT_UDP_MULTICAST:
            {
                char url[1024];
                struct in_addr in;

                in.s_addr = htonl(reply->transports[0].destination);
                snprintf(url, sizeof(url), "rtp://%s:%d?ttl=%d",
                         inet_ntoa(in),
                         reply->transports[0].port_min,
                         reply->transports[0].ttl);
                if (url_open(&rtsp_st->rtp_handle, url, URL_RDWR) < 0) {
                    err = AVERROR_INVALIDDATA;
                    goto fail;
                }
            }
            break;
        }

        if ((err = rtsp_open_transport_ctx(s, rtsp_st)))
            goto fail;
    }

    if (rt->server_type == RTSP_SERVER_REAL)
        rt->need_subscription = 1;

    return 0;

fail:
    for (i=0; i<rt->nb_rtsp_streams; i++) {
        if (rt->rtsp_streams[i]->rtp_handle) {
            url_close(rt->rtsp_streams[i]->rtp_handle);
            rt->rtsp_streams[i]->rtp_handle = NULL;
        }
    }
    return err;
}

static int rtsp_read_header(AVFormatContext *s,
                            AVFormatParameters *ap)
{
    RTSPState *rt = s->priv_data;
    char host[1024], path[1024], tcpname[1024], cmd[2048], *option_list, *option;
    URLContext *rtsp_hd;
    int port, ret, err;
    RTSPHeader reply1, *reply = &reply1;
    unsigned char *content = NULL;
    int lower_transport_mask = 0;
    char real_challenge[64];

    /* extract hostname and port */
    url_split(NULL, 0, NULL, 0,
              host, sizeof(host), &port, path, sizeof(path), s->filename);
    if (port < 0)
        port = RTSP_DEFAULT_PORT;

    /* search for options */
    option_list = strchr(path, '?');
    if (option_list) {
        /* remove the options from the path */
        *option_list++ = 0;
        while(option_list) {
            /* move the option pointer */
            option = option_list;
            option_list = strchr(option_list, '&');
            if (option_list)
                *(option_list++) = 0;
            /* handle the options */
            if (strcmp(option, "udp") == 0)
                lower_transport_mask = (1<< RTSP_LOWER_TRANSPORT_UDP);
            else if (strcmp(option, "multicast") == 0)
                lower_transport_mask = (1<< RTSP_LOWER_TRANSPORT_UDP_MULTICAST);
            else if (strcmp(option, "tcp") == 0)
                lower_transport_mask = (1<< RTSP_LOWER_TRANSPORT_TCP);
        }
    }

    if (!lower_transport_mask)
        lower_transport_mask = (1 << RTSP_LOWER_TRANSPORT_LAST) - 1;

    /* open the tcp connexion */
    snprintf(tcpname, sizeof(tcpname), "tcp://%s:%d", host, port);
    if (url_open(&rtsp_hd, tcpname, URL_RDWR) < 0)
        return AVERROR(EIO);
    rt->rtsp_hd = rtsp_hd;
    rt->seq = 0;

    /* request options supported by the server; this also detects server type */
    for (rt->server_type = RTSP_SERVER_RTP;;) {
        snprintf(cmd, sizeof(cmd),
                 "OPTIONS %s RTSP/1.0\r\n", s->filename);
        if (rt->server_type == RTSP_SERVER_REAL)
            av_strlcat(cmd,
                       /**
                        * The following entries are required for proper
                        * streaming from a Realmedia server. They are
                        * interdependent in some way although we currently
                        * don't quite understand how. Values were copied
                        * from mplayer SVN r23589.
                        * @param CompanyID is a 16-byte ID in base64
                        * @param ClientChallenge is a 16-byte ID in hex
                        */
                       "ClientChallenge: 9e26d33f2984236010ef6253fb1887f7\r\n"
                       "PlayerStarttime: [28/03/2003:22:50:23 00:00]\r\n"
                       "CompanyID: KnKV4M4I/B2FjJ1TToLycw==\r\n"
                       "GUID: 00000000-0000-0000-0000-000000000000\r\n",
                       sizeof(cmd));
        rtsp_send_cmd(s, cmd, reply, NULL);
        if (reply->status_code != RTSP_STATUS_OK) {
            err = AVERROR_INVALIDDATA;
            goto fail;
        }

        /* detect server type if not standard-compliant RTP */
        if (rt->server_type != RTSP_SERVER_REAL && reply->real_challenge[0]) {
            rt->server_type = RTSP_SERVER_REAL;
            continue;
        } else if (rt->server_type == RTSP_SERVER_REAL) {
            strcpy(real_challenge, reply->real_challenge);
        }
        break;
    }

    /* describe the stream */
    snprintf(cmd, sizeof(cmd),
             "DESCRIBE %s RTSP/1.0\r\n"
             "Accept: application/sdp\r\n",
             s->filename);
    if (rt->server_type == RTSP_SERVER_REAL) {
        /**
         * The Require: attribute is needed for proper streaming from
         * Realmedia servers.
         */
        av_strlcat(cmd,
                   "Require: com.real.retain-entity-for-setup\r\n",
                   sizeof(cmd));
    }
    rtsp_send_cmd(s, cmd, reply, &content);
    if (!content) {
        err = AVERROR_INVALIDDATA;
        goto fail;
    }
    if (reply->status_code != RTSP_STATUS_OK) {
        err = AVERROR_INVALIDDATA;
        goto fail;
    }

    /* now we got the SDP description, we parse it */
    ret = sdp_parse(s, (const char *)content);
    av_freep(&content);
    if (ret < 0) {
        err = AVERROR_INVALIDDATA;
        goto fail;
    }

    do {
        int lower_transport = ff_log2_tab[lower_transport_mask & ~(lower_transport_mask - 1)];

        err = make_setup_request(s, host, port, lower_transport,
                                 rt->server_type == RTSP_SERVER_REAL ?
                                     real_challenge : NULL);
        if (err < 0)
            goto fail;
        lower_transport_mask &= ~(1 << lower_transport);
        if (lower_transport_mask == 0 && err == 1) {
            err = AVERROR(FF_NETERROR(EPROTONOSUPPORT));
            goto fail;
        }
    } while (err);

    rt->state = RTSP_STATE_IDLE;
    rt->seek_timestamp = 0; /* default is to start stream at position
                               zero */
    if (ap->initial_pause) {
        /* do not start immediately */
    } else {
        if (rtsp_read_play(s) < 0) {
            err = AVERROR_INVALIDDATA;
            goto fail;
        }
    }
    return 0;
 fail:
    rtsp_close_streams(rt);
    av_freep(&content);
    url_close(rt->rtsp_hd);
    return err;
}

static int tcp_read_packet(AVFormatContext *s, RTSPStream **prtsp_st,
                           uint8_t *buf, int buf_size)
{
    RTSPState *rt = s->priv_data;
    int id, len, i, ret;
    RTSPStream *rtsp_st;

#ifdef DEBUG_RTP_TCP
    printf("tcp_read_packet:\n");
#endif
 redo:
    for(;;) {
        ret = url_readbuf(rt->rtsp_hd, buf, 1);
#ifdef DEBUG_RTP_TCP
        printf("ret=%d c=%02x [%c]\n", ret, buf[0], buf[0]);
#endif
        if (ret != 1)
            return -1;
        if (buf[0] == '$')
            break;
    }
    ret = url_readbuf(rt->rtsp_hd, buf, 3);
    if (ret != 3)
        return -1;
    id = buf[0];
    len = AV_RB16(buf + 1);
#ifdef DEBUG_RTP_TCP
    printf("id=%d len=%d\n", id, len);
#endif
    if (len > buf_size || len < 12)
        goto redo;
    /* get the data */
    ret = url_readbuf(rt->rtsp_hd, buf, len);
    if (ret != len)
        return -1;
    if (rt->transport == RTSP_TRANSPORT_RDT &&
        ff_rdt_parse_header(buf, len, &id, NULL, NULL, NULL, NULL) < 0)
        return -1;

    /* find the matching stream */
    for(i = 0; i < rt->nb_rtsp_streams; i++) {
        rtsp_st = rt->rtsp_streams[i];
        if (id >= rtsp_st->interleaved_min &&
            id <= rtsp_st->interleaved_max)
            goto found;
    }
    goto redo;
 found:
    *prtsp_st = rtsp_st;
    return len;
}

static int udp_read_packet(AVFormatContext *s, RTSPStream **prtsp_st,
                           uint8_t *buf, int buf_size)
{
    RTSPState *rt = s->priv_data;
    RTSPStream *rtsp_st;
    fd_set rfds;
    int fd1, fd2, fd_max, n, i, ret;
    struct timeval tv;

    for(;;) {
        if (url_interrupt_cb())
            return AVERROR(EINTR);
        FD_ZERO(&rfds);
        fd_max = -1;
        for(i = 0; i < rt->nb_rtsp_streams; i++) {
            rtsp_st = rt->rtsp_streams[i];
            /* currently, we cannot probe RTCP handle because of blocking restrictions */
            rtp_get_file_handles(rtsp_st->rtp_handle, &fd1, &fd2);
            if (fd1 > fd_max)
                fd_max = fd1;
            FD_SET(fd1, &rfds);
        }
        tv.tv_sec = 0;
        tv.tv_usec = 100 * 1000;
        n = select(fd_max + 1, &rfds, NULL, NULL, &tv);
        if (n > 0) {
            for(i = 0; i < rt->nb_rtsp_streams; i++) {
                rtsp_st = rt->rtsp_streams[i];
                rtp_get_file_handles(rtsp_st->rtp_handle, &fd1, &fd2);
                if (FD_ISSET(fd1, &rfds)) {
                    ret = url_read(rtsp_st->rtp_handle, buf, buf_size);
                    if (ret > 0) {
                        *prtsp_st = rtsp_st;
                        return ret;
                    }
                }
            }
        }
    }
}

static int rtsp_read_packet(AVFormatContext *s,
                            AVPacket *pkt)
{
    RTSPState *rt = s->priv_data;
    RTSPStream *rtsp_st;
    int ret, len;
    uint8_t buf[RTP_MAX_PACKET_LENGTH];

    if (rt->server_type == RTSP_SERVER_REAL && rt->need_subscription) {
        int i;
        RTSPHeader reply1, *reply = &reply1;
        char cmd[1024];

        snprintf(cmd, sizeof(cmd),
                 "SET_PARAMETER %s RTSP/1.0\r\n"
                 "Subscribe: ",
                 s->filename);
        for (i = 0; i < rt->nb_rtsp_streams; i++) {
            if (i != 0) av_strlcat(cmd, ",", sizeof(cmd));
            ff_rdt_subscribe_rule(cmd, sizeof(cmd), i, 0);
            if (rt->transport == RTSP_TRANSPORT_RDT)
                ff_rdt_subscribe_rule2(
                    rt->rtsp_streams[i]->tx_ctx,
                    cmd, sizeof(cmd), i, 0);
        }
        av_strlcat(cmd, "\r\n", sizeof(cmd));
        rtsp_send_cmd(s, cmd, reply, NULL);
        if (reply->status_code != RTSP_STATUS_OK)
            return AVERROR_INVALIDDATA;
        rt->need_subscription = 0;

        if (rt->state == RTSP_STATE_PLAYING)
            rtsp_read_play (s);
    }

    /* get next frames from the same RTP packet */
    if (rt->cur_tx) {
        if (rt->transport == RTSP_TRANSPORT_RDT)
            ret = ff_rdt_parse_packet(rt->cur_tx, pkt, NULL, 0);
        else
            ret = rtp_parse_packet(rt->cur_tx, pkt, NULL, 0);
        if (ret == 0) {
            rt->cur_tx = NULL;
            return 0;
        } else if (ret == 1) {
            return 0;
        } else {
            rt->cur_tx = NULL;
        }
    }

    /* read next RTP packet */
 redo:
    switch(rt->lower_transport) {
    default:
    case RTSP_LOWER_TRANSPORT_TCP:
        len = tcp_read_packet(s, &rtsp_st, buf, sizeof(buf));
        break;
    case RTSP_LOWER_TRANSPORT_UDP:
    case RTSP_LOWER_TRANSPORT_UDP_MULTICAST:
        len = udp_read_packet(s, &rtsp_st, buf, sizeof(buf));
        if (len >=0 && rtsp_st->tx_ctx && rt->transport == RTSP_TRANSPORT_RTP)
            rtp_check_and_send_back_rr(rtsp_st->tx_ctx, len);
        break;
    }
    if (len < 0)
        return len;
    if (rt->transport == RTSP_TRANSPORT_RDT)
        ret = ff_rdt_parse_packet(rtsp_st->tx_ctx, pkt, buf, len);
    else
        ret = rtp_parse_packet(rtsp_st->tx_ctx, pkt, buf, len);
    if (ret < 0)
        goto redo;
    if (ret == 1) {
        /* more packets may follow, so we save the RTP context */
        rt->cur_tx = rtsp_st->tx_ctx;
    }
    return 0;
}

static int rtsp_read_play(AVFormatContext *s)
{
    RTSPState *rt = s->priv_data;
    RTSPHeader reply1, *reply = &reply1;
    char cmd[1024];

    av_log(s, AV_LOG_DEBUG, "hello state=%d\n", rt->state);

    if (!(rt->server_type == RTSP_SERVER_REAL && rt->need_subscription)) {
        if (rt->state == RTSP_STATE_PAUSED) {
            snprintf(cmd, sizeof(cmd),
                     "PLAY %s RTSP/1.0\r\n",
                     s->filename);
        } else {
            snprintf(cmd, sizeof(cmd),
                     "PLAY %s RTSP/1.0\r\n"
                     "Range: npt=%0.3f-\r\n",
                     s->filename,
                     (double)rt->seek_timestamp / AV_TIME_BASE);
        }
        rtsp_send_cmd(s, cmd, reply, NULL);
        if (reply->status_code != RTSP_STATUS_OK) {
            return -1;
        }
    }
    rt->state = RTSP_STATE_PLAYING;
    return 0;
}

/* pause the stream */
static int rtsp_read_pause(AVFormatContext *s)
{
    RTSPState *rt = s->priv_data;
    RTSPHeader reply1, *reply = &reply1;
    char cmd[1024];

    rt = s->priv_data;

    if (rt->state != RTSP_STATE_PLAYING)
        return 0;
    else if (!(rt->server_type == RTSP_SERVER_REAL && rt->need_subscription)) {
        snprintf(cmd, sizeof(cmd),
                 "PAUSE %s RTSP/1.0\r\n",
                 s->filename);
        rtsp_send_cmd(s, cmd, reply, NULL);
        if (reply->status_code != RTSP_STATUS_OK) {
            return -1;
        }
    }
    rt->state = RTSP_STATE_PAUSED;
    return 0;
}

static int rtsp_read_seek(AVFormatContext *s, int stream_index,
                          int64_t timestamp, int flags)
{
    RTSPState *rt = s->priv_data;

    rt->seek_timestamp = av_rescale_q(timestamp, s->streams[stream_index]->time_base, AV_TIME_BASE_Q);
    switch(rt->state) {
    default:
    case RTSP_STATE_IDLE:
        break;
    case RTSP_STATE_PLAYING:
        if (rtsp_read_play(s) != 0)
            return -1;
        break;
    case RTSP_STATE_PAUSED:
        rt->state = RTSP_STATE_IDLE;
        break;
    }
    return 0;
}

static int rtsp_read_close(AVFormatContext *s)
{
    RTSPState *rt = s->priv_data;
    RTSPHeader reply1, *reply = &reply1;
    char cmd[1024];

#if 0
    /* NOTE: it is valid to flush the buffer here */
    if (rt->lower_transport == RTSP_LOWER_TRANSPORT_TCP) {
        url_fclose(&rt->rtsp_gb);
    }
#endif
    snprintf(cmd, sizeof(cmd),
             "TEARDOWN %s RTSP/1.0\r\n",
             s->filename);
    rtsp_send_cmd(s, cmd, reply, NULL);

    rtsp_close_streams(rt);
    url_close(rt->rtsp_hd);
    return 0;
}

#ifdef CONFIG_RTSP_DEMUXER
AVInputFormat rtsp_demuxer = {
    "rtsp",
    NULL_IF_CONFIG_SMALL("RTSP input format"),
    sizeof(RTSPState),
    rtsp_probe,
    rtsp_read_header,
    rtsp_read_packet,
    rtsp_read_close,
    rtsp_read_seek,
    .flags = AVFMT_NOFILE,
    .read_play = rtsp_read_play,
    .read_pause = rtsp_read_pause,
};
#endif

static int sdp_probe(AVProbeData *p1)
{
    const char *p = p1->buf, *p_end = p1->buf + p1->buf_size;

    /* we look for a line beginning "c=IN IP4" */
    while (p < p_end && *p != '\0') {
        if (p + sizeof("c=IN IP4") - 1 < p_end && av_strstart(p, "c=IN IP4", NULL))
            return AVPROBE_SCORE_MAX / 2;

        while(p < p_end - 1 && *p != '\n') p++;
        if (++p >= p_end)
            break;
        if (*p == '\r')
            p++;
    }
    return 0;
}

#define SDP_MAX_SIZE 8192

static int sdp_read_header(AVFormatContext *s,
                           AVFormatParameters *ap)
{
    RTSPState *rt = s->priv_data;
    RTSPStream *rtsp_st;
    int size, i, err;
    char *content;
    char url[1024];

    /* read the whole sdp file */
    /* XXX: better loading */
    content = av_malloc(SDP_MAX_SIZE);
    size = get_buffer(s->pb, content, SDP_MAX_SIZE - 1);
    if (size <= 0) {
        av_free(content);
        return AVERROR_INVALIDDATA;
    }
    content[size] ='\0';

    sdp_parse(s, content);
    av_free(content);

    /* open each RTP stream */
    for(i=0;i<rt->nb_rtsp_streams;i++) {
        rtsp_st = rt->rtsp_streams[i];

        snprintf(url, sizeof(url), "rtp://%s:%d?localport=%d&ttl=%d",
                 inet_ntoa(rtsp_st->sdp_ip),
                 rtsp_st->sdp_port,
                 rtsp_st->sdp_port,
                 rtsp_st->sdp_ttl);
        if (url_open(&rtsp_st->rtp_handle, url, URL_RDWR) < 0) {
            err = AVERROR_INVALIDDATA;
            goto fail;
        }
        if ((err = rtsp_open_transport_ctx(s, rtsp_st)))
            goto fail;
    }
    return 0;
 fail:
    rtsp_close_streams(rt);
    return err;
}

static int sdp_read_packet(AVFormatContext *s,
                            AVPacket *pkt)
{
    return rtsp_read_packet(s, pkt);
}

static int sdp_read_close(AVFormatContext *s)
{
    RTSPState *rt = s->priv_data;
    rtsp_close_streams(rt);
    return 0;
}

#ifdef CONFIG_SDP_DEMUXER
AVInputFormat sdp_demuxer = {
    "sdp",
    NULL_IF_CONFIG_SMALL("SDP"),
    sizeof(RTSPState),
    sdp_probe,
    sdp_read_header,
    sdp_read_packet,
    sdp_read_close,
};
#endif

#ifdef CONFIG_REDIR_DEMUXER
/* dummy redirector format (used directly in av_open_input_file now) */
static int redir_probe(AVProbeData *pd)
{
    const char *p;
    p = pd->buf;
    while (redir_isspace(*p))
        p++;
    if (av_strstart(p, "http://", NULL) ||
        av_strstart(p, "rtsp://", NULL))
        return AVPROBE_SCORE_MAX;
    return 0;
}

static int redir_read_header(AVFormatContext *s, AVFormatParameters *ap)
{
    char buf[4096], *q;
    int c;
    AVFormatContext *ic = NULL;
    ByteIOContext *f = s->pb;

    /* parse each URL and try to open it */
    c = url_fgetc(f);
    while (c != URL_EOF) {
        /* skip spaces */
        for(;;) {
            if (!redir_isspace(c))
                break;
            c = url_fgetc(f);
        }
        if (c == URL_EOF)
            break;
        /* record url */
        q = buf;
        for(;;) {
            if (c == URL_EOF || redir_isspace(c))
                break;
            if ((q - buf) < sizeof(buf) - 1)
                *q++ = c;
            c = url_fgetc(f);
        }
        *q = '\0';
        //printf("URL='%s'\n", buf);
        /* try to open the media file */
        if (av_open_input_file(&ic, buf, NULL, 0, NULL) == 0)
            break;
    }
    if (!ic)
        return AVERROR(EIO);

    *s = *ic;
    url_fclose(f);

    return 0;
}

AVInputFormat redir_demuxer = {
    "redir",
    NULL_IF_CONFIG_SMALL("Redirector format"),
    0,
    redir_probe,
    redir_read_header,
    NULL,
    NULL,
};
#endif
