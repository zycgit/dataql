/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */

module.exports = function () {
    return {
        name: 'docusaurus-plugin',
        injectHtmlTags() {
            return {
                postBodyTags: [`
<!-- 百度统计 -->
<script>
    var _hmt = _hmt || [];
    (function () {
        var hm = document.createElement("script");
        hm.src = "https://hm.baidu.com/hm.js?1f0226555f5221c8930b94c9f1ac713d";
        var s = document.getElementsByTagName("script")[0];
        s.parentNode.insertBefore(hm, s);
    })();
</script>
<!-- 备案审核 -->
<script>
var tmpTitle = window.location.pathname;

function trackView(){
    if (_hmt == null) {
        return;
    }
    if (tmpTitle != window.location.pathname) {
        try {
            _hmt.push(['_trackPageview', window.location.pathname]); 
        } catch (e) {
        } finally {
            tmpTitle = window.location.pathname;
        }
    }
}

function setTitle(){
    trackView();
    window.setTimeout(setTitle,100);
}
window.setTimeout(setTitle,100);
</script>
`],
            };
        },
    };
};