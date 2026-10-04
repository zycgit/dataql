/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import React from 'react';
import clsx from 'clsx';
import {useBlogPost} from '@docusaurus/plugin-content-blog/client';
import OriginalBlogPostItem from '@theme-original/BlogPostItem';
import BlogPostItemHeader from '@theme/BlogPostItem/Header';
import BlogPostItemContent from '@theme/BlogPostItem/Content';
import ReadMoreLink from '@theme/BlogPostItem/Footer/ReadMoreLink';
import styles from './styles.module.css';

export default function BlogPostItem({children, className}) {
    const {isBlogPostPage, metadata} = useBlogPost();
    if (isBlogPostPage) {
        return <OriginalBlogPostItem className={className}>{children}</OriginalBlogPostItem>;
    }
    return (
        <article className={clsx(styles.listItem, className)}>
            <BlogPostItemHeader />
            <BlogPostItemContent>{children}</BlogPostItemContent>
            {metadata.hasTruncateMarker && (
                <footer className={styles.readMore}>
                    <ReadMoreLink blogPostTitle={metadata.title} to={metadata.permalink} />
                    <span aria-hidden="true"> →</span>
                </footer>
            )}
        </article>
    );
}
