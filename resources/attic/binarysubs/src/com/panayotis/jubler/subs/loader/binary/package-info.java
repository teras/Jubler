/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * This file is part of Jubler.
 *
 */

/**
 * This package contains classes that are used to load binary or sub-binary
 * format files. An instance of such is the DVDMaestro's SON format files.
 * Although the subtitle index file holds a pure textual content, the
 * attachement of images qualifies it to be a binary format. The parsing
 * mechanism for classes in this package is more complex than the one presented
 * in the {@link com.panayotis.jubler.subs.loader.AbstractTextSubFormat} and it
 * is done so to adapt to the complex format of data the subtitle files
 * inherited. Example of such instance are files with different blocks and each
 * blocks contains values with different meaning. <br><br>
 * The parsing mechanism represented in the
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat} is used with
 * a list of {@link com.panayotis.jubler.subs.SubtitlePatternProcessor}, each in
 * charge of processing one pattern. The parsing mechanism also generates
 * several events in which instances of
 * {@link com.panayotis.jubler.subs.loader.AbstractTextSubFormat} can catch to
 * transform, manipulate data or alter the state of processing.
 */
package com.panayotis.jubler.subs.loader.binary;
