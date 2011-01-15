/*
 * filter graphs
 * copyright (c) 2008 Vitor Sessak
 * copyright (c) 2007 Bobby Bingham
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

#include <ctype.h>
#include <string.h>

#include "avfilter.h"
#include "avfiltergraph.h"

void avfilter_destroy_graph(AVFilterGraph *graph)
{
    for(; graph->filter_count > 0; graph->filter_count --)
        avfilter_destroy(graph->filters[graph->filter_count - 1]);
    av_freep(&graph->filters);
}

int avfilter_graph_add_filter(AVFilterGraph *graph, AVFilterContext *filter)
{
    graph->filters = av_realloc(graph->filters,
                                sizeof(AVFilterContext*) * ++graph->filter_count);

    if (!graph->filters)
        return -1;

    graph->filters[graph->filter_count - 1] = filter;

    return 0;
}

AVFilterContext *avfilter_graph_get_filter(AVFilterGraph *graph, char *name)
{
    int i;

    for(i = 0; i < graph->filter_count; i ++)
        if(graph->filters[i]->name && !strcmp(name, graph->filters[i]->name))
            return graph->filters[i];

    return NULL;
}

static int query_formats(AVFilterGraph *graph)
{
    int i, j;
    int scaler_count = 0;
    char inst_name[30];

    /* ask all the sub-filters for their supported colorspaces */
    for(i = 0; i < graph->filter_count; i ++) {
        if(graph->filters[i]->filter->query_formats)
            graph->filters[i]->filter->query_formats(graph->filters[i]);
        else
            avfilter_default_query_formats(graph->filters[i]);
    }

    /* go through and merge as many format lists as possible */
    for(i = 0; i < graph->filter_count; i ++) {
        AVFilterContext *filter = graph->filters[i];

        for(j = 0; j < filter->input_count; j ++) {
            AVFilterLink *link = filter->inputs[j];
            if(link && link->in_formats != link->out_formats) {
                if(!avfilter_merge_formats(link->in_formats,
                                           link->out_formats)) {
                    AVFilterContext *scale;
                    /* couldn't merge format lists. auto-insert scale filter */
                    snprintf(inst_name, sizeof(inst_name), "auto-inserted scaler %d",
                             scaler_count);
                    scale =
                        avfilter_open(avfilter_get_by_name("scale"),inst_name);

                    if(!scale || scale->filter->init(scale, NULL, NULL) ||
                                 avfilter_insert_filter(link, scale, 0, 0)) {
                        avfilter_destroy(scale);
                        return -1;
                    }

                    if (avfilter_graph_add_filter(graph, scale) < 0)
                        return -1;

                    scale->filter->query_formats(scale);
                    if(!avfilter_merge_formats(scale-> inputs[0]->in_formats,
                                               scale-> inputs[0]->out_formats)||
                       !avfilter_merge_formats(scale->outputs[0]->in_formats,
                                               scale->outputs[0]->out_formats))
                        return -1;
                }
            }
        }
    }

    return 0;
}

static void pick_format(AVFilterLink *link)
{
    if(!link || !link->in_formats)
        return;

    link->in_formats->format_count = 1;
    link->format = link->in_formats->formats[0];

    avfilter_formats_unref(&link->in_formats);
    avfilter_formats_unref(&link->out_formats);
}

static void pick_formats(AVFilterGraph *graph)
{
    int i, j;

    for(i = 0; i < graph->filter_count; i ++) {
        AVFilterContext *filter = graph->filters[i];

        for(j = 0; j < filter->input_count; j ++)
            pick_format(filter->inputs[j]);
        for(j = 0; j < filter->output_count; j ++)
            pick_format(filter->outputs[j]);
    }
}

int avfilter_graph_config_formats(AVFilterGraph *graph)
{
    /* find supported formats from sub-filters, and merge along links */
    if(query_formats(graph))
        return -1;

    /* Once everything is merged, it's possible that we'll still have
     * multiple valid colorspace choices. We pick the first one. */
    pick_formats(graph);

    return 0;
}

