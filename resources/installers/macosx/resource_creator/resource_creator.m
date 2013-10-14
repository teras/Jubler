#include <AppKit/AppKit.h>
#import <Foundation/Foundation.h>

// Original authors:
// http://gigliwood.com/weblog/cocoa/Converting_Rich_Tex.html  
// https://github.com/pypt/FreeDMG/blob/master/rtf2r.m  under GNU GPL

#define PREFIX "data 'TMPL' (128, \"LPic\") {\n\
    $\"1344 6566 6175 6C74 204C 616E 6775 6167\"    /* .Default Languag */\n\
    $\"6520 4944 4457 5244 0543 6F75 6E74 4F43\"    /* e IDDWRD.CountOC */\n\
    $\"4E54 042A 2A2A 2A4C 5354 430B 7379 7320\"    /* NT.****LSTC.sys  */\n\
    $\"6C61 6E67 2049 4444 5752 441E 6C6F 6361\"    /* lang IDDWRD.loca */\n\
    $\"6C20 7265 7320 4944 2028 6F66 6673 6574\"    /* l res ID (offset */\n\
    $\"2066 726F 6D20 3530 3030 4457 5244 1032\"    /*  from 5000DWRD.2 */\n\
    $\"2D62 7974 6520 6C61 6E67 7561 6765 3F44\"    /* -byte language?D */\n\
    $\"5752 4404 2A2A 2A2A 4C53 5445\"              /* WRD.****LSTE */\n\
};\n\
\n\
data 'LPic' (5000) {\n\
    $\"0000 0002 0000 0000 0000 0000 0004 0000\"    /* ................ */\n\
};\n\
\n\
data 'STR#' (5000, \"English buttons\") {\n\
    $\"0006 0D45 6E67 6C69 7368 2074 6573 7431\"    /* ...English test1 */\n\
    $\"0541 6772 6565 0844 6973 6167 7265 6505\"    /* .Agree.Disagree. */\n\
    $\"5072 696E 7407 5361 7665 2E2E 2E7A 4966\"    /* Print.Save...zIf */\n\
    $\"2079 6F75 2061 6772 6565 2077 6974 6820\"    /*  you agree with  */\n\
    $\"7468 6520 7465 726D 7320 6F66 2074 6869\"    /* the terms of thi */\n\
    $\"7320 6C69 6365 6E73 652C 2063 6C69 636B\"    /* s license, click */\n\
    $\"2022 4167 7265 6522 2074 6F20 6163 6365\"    /*  \"Agree\" to acce */\n\
    $\"7373 2074 6865 2073 6F66 7477 6172 652E\"    /* ss the software. */\n\
    $\"2020 4966 2079 6F75 2064 6F20 6E6F 7420\"    /*   If you do not  */\n\
    $\"6167 7265 652C 2070 7265 7373 2022 4469\"    /* agree, press \"Di */\n\
    $\"7361 6772 6565 2E22\"                        /* sagree.\" */\n\
};\n\
\n\
data 'STR#' (5002, \"English\") {\n\
    $\"0006 0745 6E67 6C69 7368 0541 6772 6565\"    /* ...English.Agree */\n\
    $\"0844 6973 6167 7265 6505 5072 696E 7407\"    /* .Disagree.Print. */\n\
    $\"5361 7665 2E2E 2E7B 4966 2079 6F75 2061\"    /* Save...{If you a */\n\
    $\"6772 6565 2077 6974 6820 7468 6520 7465\"    /* gree with the te */\n\
    $\"726D 7320 6F66 2074 6869 7320 6C69 6365\"    /* rms of this lice */\n\
    $\"6E73 652C 2070 7265 7373 2022 4167 7265\"    /* nse, press \"Agre */\n\
    $\"6522 2074 6F20 696E 7374 616C 6C20 7468\"    /* e\" to install th */\n\
    $\"6520 736F 6674 7761 7265 2E20 2049 6620\"    /* e software.  If  */\n\
    $\"796F 7520 646F 206E 6F74 2061 6772 6565\"    /* you do not agree */\n\
    $\"2C20 7072 6573 7320 2244 6973 6167 7265\"    /* , press \"Disagre */\n\
    $\"6522 2E\"                                    /* e\". */\
};\n\n"


void dump_rsrc(const char *type, NSData *data)
{
    short num, cnt;
    Size size, i, j;
    const unsigned char *buf = [data bytes];
    
    size = [data length];
    printf("data '%s' (5000, \"English\") {\n", type);
    
    for (i = 0; i < size; i += 16) {
        num = (i + 16 <= size) ? 16 : size - i;
        printf("    $\"");
        cnt = 0;
        for (j = 0; j < num; ++j) {
            if (j > 0 && (j & 1) == 0) {
                printf(" ");
                ++cnt;
            }
            printf("%02X", buf[i + j]);
            cnt += 2;
        }
        
        printf("\"");
        for ( ; cnt < 39 + 4; ++cnt) {
            printf(" ");
        }
        printf("/* ");
        for (j = 0; j < num; ++j) {
            printf("%c", isprint(buf[i + j]) ? buf[i + j] : '.');
        }
        printf(" */\n");
    }
    
    printf("};\n");
    printf("\n");
}

int main (int argc, const char * argv[]) {
    
    if (2 != argc) {
        fprintf(stderr, "Usage: %s RTF_FILE\n", argv[0]);
        return -1;
    }
    
    NSString *sourcePath = [NSString stringWithUTF8String:argv[1]];
    NSAttributedString *str = [[NSAttributedString alloc] initWithPath:sourcePath documentAttributes:nil];
    NSData *data = [str RTFFromRange:NSMakeRange(0, [str length]) documentAttributes:nil];
    
    NSPasteboard *pb = [NSPasteboard generalPasteboard];
    [pb declareTypes:[NSArray arrayWithObject:NSRTFPboardType] owner:nil];
    [pb setData:data forType:NSRTFPboardType];
    
    // Create data
    NSData *textData  = [pb dataForType:@"CorePasteboardFlavorType 0x54455854"];   // TEXT
    NSData *styleData = [pb dataForType:@"CorePasteboardFlavorType 0x7374796C"];   // styl
    
    // Flip data due to endianess
    unsigned long len = [styleData length];
    char *bytes = malloc(len);
    [styleData getBytes:bytes length:len];
    
   CoreEndianFlipData (kCoreEndianResourceManagerDomain, 'styl', 0, bytes, len, false);
    
    NSData *newStyleData = [[NSData alloc] initWithBytesNoCopy:bytes length:len freeWhenDone:YES];
 
    printf(PREFIX);   
    dump_rsrc("TEXT", textData);
    dump_rsrc("styl", newStyleData);
    
    return 0;
}
