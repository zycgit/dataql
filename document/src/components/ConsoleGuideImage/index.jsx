/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import React, {useId} from 'react';
import useBaseUrl from '@docusaurus/useBaseUrl';
import styles from './styles.module.css';

const regions = {
    list: [
        {label: 'A  API list', box: [3, 62, 713, 854], tag: [220, 390], arrow: [320, 390, 195, 318]},
        {label: 'B  Request', box: [723, 62, 713, 424], tag: [1030, 350], arrow: [1125, 350, 970, 103]},
        {label: 'C  Result', box: [723, 493, 713, 423], tag: [1090, 795], arrow: [1185, 795, 1020, 718]},
    ],
    editor: [
        {label: 'A  Toolbar', box: [3, 62, 1433, 34], tag: [1040, 210], arrow: [1135, 210, 1160, 98]},
        {label: 'B  Script', box: [3, 102, 713, 814], tag: [220, 390], arrow: [315, 390, 350, 111]},
        {label: 'C  Parameters', box: [723, 102, 713, 404], tag: [1030, 350], arrow: [1030, 372, 990, 160]},
        {label: 'D  Result', box: [723, 513, 713, 403], tag: [1090, 815], arrow: [1185, 815, 1020, 738]},
    ],
    'result-handlers': [
        {label: '1  Result Handler', box: [1242, 510, 106, 31], tag: [965, 405], arrow: [1170, 427, 1295, 510]},
        {label: '2  Output modes', box: [1128, 547, 220, 212], tag: [955, 805], arrow: [1120, 805, 1238, 759]},
    ],
    history: [
        {label: '1  History', box: [1253, 65, 47, 29], tag: [1025, 9], arrow: [1230, 32, 1276, 65]},
        {label: '2  Restore', box: [1372, 193, 29, 29], tag: [1190, 360], arrow: [1292, 360, 1387, 223]},
    ],
    disable: [
        {label: '1  Disable API', box: [1295, 65, 47, 29], tag: [1025, 9], arrow: [1230, 32, 1318, 65]},
        {label: '2  Confirm', box: [864, 478, 56, 36], tag: [955, 610], arrow: [1000, 610, 892, 515]},
    ],
    delete: [
        {label: '1  Delete API', box: [1295, 65, 47, 29], tag: [1025, 9], arrow: [1230, 32, 1318, 65]},
        {label: '2  Confirm', box: [864, 478, 56, 36], tag: [955, 610], arrow: [1000, 610, 892, 515]},
    ],
};

export default function ConsoleGuideImage({page, alt}) {
    const src = useBaseUrl(`/img/dataway/console-${page}.png`);
    const markerId = useId();
    return (
        <figure className={styles.figure}>
            <img src={src} alt={alt} width="1440" height="920" loading="lazy" />
            <svg viewBox="0 0 1440 920" aria-hidden="true" className={styles.annotations}>
                <defs>
                    <marker id={markerId} markerWidth="10" markerHeight="10" refX="9" refY="5" orient="auto">
                        <path d="M 0 0 L 10 5 L 0 10 Z" fill="#2563eb" />
                    </marker>
                </defs>
                {regions[page].map(({label, box, tag, arrow}) => (
                    <g key={label}>
                        <rect x={box[0]} y={box[1]} width={box[2]} height={box[3]} rx="3"
                              fill="none" stroke="#2563eb" strokeWidth="2" />
                        <line x1={arrow[0]} y1={arrow[1]} x2={arrow[2]} y2={arrow[3]}
                              stroke="#2563eb" strokeWidth="2" markerEnd={`url(#${markerId})`} />
                        <rect x={tag[0]} y={tag[1]} width="205" height="44" rx="6" fill="#2563eb" />
                        <text x={tag[0] + 102.5} y={tag[1] + 29} textAnchor="middle"
                              fill="white" fontSize="24" fontWeight="600" fontFamily="Arial, sans-serif">{label}</text>
                    </g>
                ))}
            </svg>
        </figure>
    );
}
