/*
 * Filter graphs
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

#ifndef AVFILTER_AVFILTERGRAPH_H
#define AVFILTER_AVFILTERGRAPH_H

#include "avfilter.h"

typedef struct AVFilterGraph {
    unsigned filter_count;
    AVFilterContext **filters;
} AVFilterGraph;

/**
 * Get a pointer to a graph by instance name
 */
AVFilterContext *avfilter_graph_get_filter(AVFilterGraph *graph, char *name);

/**
 * Add an existing filter instance to a filter graph.
 * @param graph  The filter graph
 * @param filter The filter to be added
 */
int avfilter_graph_add_filter(AVFilterGraph *graphctx, AVFilterContext *filter);

/**
 * Configure the formats of all the links in the graph.
 */
int avfilter_graph_config_formats(AVFilterGraph *graphctx);

/**
 * Free a graph and destroy its links.
 */
void avfilter_destroy_graph(AVFilterGraph *graph);

#endif  /* AVFILTER_AVFILTERGRAPH_H */
