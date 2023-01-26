import React from 'react';
import clsx from 'clsx';
import styles from './HomepageFeatures.module.css';
import Translate, {translate} from '@docusaurus/Translate';

const FeatureList = [
    {
        title: translate({id: 'homepage.feature1_title', message: '结构化'}),
        Svg: require('../../static/img/undraw_docusaurus_mountain.svg').default,
        description: (
            <><Translate id="homepage.feature1_desc">数据为中心、数据结构层次化</Translate></>
        ),
    },
    {
        title: translate({id: 'homepage.feature2_title', message: '简单好用'}),
        Svg: require('../../static/img/undraw_docusaurus_tree.svg').default,
        description: (
            <><Translate id="homepage.feature2_desc">数据结构描述直观、弱类型、简单的逻辑表达</Translate></>
        ),
    },
    {
        title: translate({id: 'homepage.feature6_title', message: '混合语言'}),
        Svg: require('../../static/img/undraw_docusaurus_react.svg').default,
        description: (
            <><Translate id="homepage.feature6_desc">允许查询中混合任意的其它语言代码，典型的场景是查询中混合 SQL 查询语句</Translate></>
        ),
    },
];

function Feature({Svg, title, description}) {
    return (
        <div className={clsx('col col--4')}>
            <div className="text--center">
                <Svg className={styles.featureSvg} alt={title}/>
            </div>
            <div className="text--center padding-horiz--md">
                <h3>{title}</h3>
                <p>{description}</p>
            </div>
        </div>
    );
}

export default function HomepageFeatures() {
    return (
        <section className={styles.features}>
            <div className="container">
                <div className="row">
                    {FeatureList.map((props, idx) => (
                        <Feature key={idx} {...props} />
                    ))}
                </div>
            </div>
        </section>
    );
}
