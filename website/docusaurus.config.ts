import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const repositoryUrl = 'https://github.com/onec-devops/onec-gradle-plugin';
const config: Config = {
  title: 'Onec Gradle Plugin',
  tagline: 'Gradle support for 1C:Enterprise development',
  favicon: 'img/favicon.png',

  future: {
    v4: true,
  },

  url: 'https://onec-devops.github.io',
  baseUrl: '/onec-gradle-plugin/',
  organizationName: 'onec-devops',
  projectName: 'onec-gradle-plugin',
  trailingSlash: false,

  onBrokenLinks: 'throw',
  onBrokenAnchors: 'throw',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'throw',
    },
  },

  i18n: {
    defaultLocale: 'en',
    locales: ['en', 'ru'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: '/',
          sidebarPath: './sidebars.ts',
          editUrl: `${repositoryUrl}/edit/main/website/`,
          editLocalizedFiles: true,
        },
        blog: false,
        pages: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themes: [
    [
      '@easyops-cn/docusaurus-search-local',
      {
        hashed: true,
        language: ['en', 'ru'],
        docsRouteBasePath: '/',
        indexBlog: false,
        indexPages: false,
      },
    ],
  ],

  themeConfig: {
    image: 'img/social-card.png',
    metadata: [{name: 'google-site-verification', content: '5-91D25BYoEIEzKuLl7CnIBHcdfu2G86v9u8MWMJSho'}],
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'Onec Gradle Plugin',
      logo: {
        alt: '',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docs',
          label: 'Docs',
          position: 'left',
        },
        {
          type: 'docsVersionDropdown',
          position: 'right',
          dropdownItemsAfter: [
            {
              type: 'html',
              value: '<hr style="margin: 0.3rem 0;">',
            },
            {
              href: `${repositoryUrl}/releases`,
              label: 'GitHub Releases',
            },
          ],
        },
        {
          type: 'localeDropdown',
          position: 'right',        },
        {
          href: repositoryUrl,
          position: 'right',
          className: 'header-github-link',
          'aria-label': 'GitHub',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          label: 'GitHub',
          href: repositoryUrl,
        },
        {
          label: 'Contributing',
          href: `${repositoryUrl}/blob/main/CONTRIBUTING.md`,
        },
        {
          label: 'Gradle Plugin Portal',
          href: 'https://plugins.gradle.org/plugin/io.github.onec-devops.onec',
        },
      ],
      copyright: 'Distributed under the Apache License 2.0.',
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['groovy', 'powershell', 'properties'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
