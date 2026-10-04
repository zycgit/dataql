/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import React from 'react';
import BlogSidebarContent from '@theme-original/BlogSidebar/Content';
import BlogBrowseLinks from '@site/src/components/BlogBrowseLinks';

// The mobile sidebar uses this shared content inside its navigation drawer.
export default function BlogSidebarContentWithLinks(props) {
    return (
        <>
            <BlogBrowseLinks />
            <BlogSidebarContent {...props} />
        </>
    );
}
