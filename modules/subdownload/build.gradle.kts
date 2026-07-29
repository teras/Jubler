plugins {
    id("jubler.plugin-conventions")
}

// The subtitle downloader: the public provider SPI + HTTP/extraction toolkit that providers compile
// against, the downloader UI (frame + Tools-menu entry), and provider-support helpers. The concrete
// providers live in their own sibling modules; out-of-tree providers depend on this module too.
