/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import React from 'react';
import BlogPostPage from '@theme-original/BlogPostPage';

export default function ArticlePage(props) {
    return <BlogPostPage {...props} sidebar={{...props.sidebar, items: []}} />;
}
