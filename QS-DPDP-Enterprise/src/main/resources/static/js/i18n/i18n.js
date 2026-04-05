/**
 * QS-DPDP Enterprise — Internationalization (i18n) Engine
 * Dynamically translates UI labels based on selected language.
 * Supports: English, Hindi, Tamil, Telugu, Bengali, Marathi
 *
 * Usage:
 *   await QSI18n.init('hi');           // Initialize with Hindi
 *   QSI18n.t('nav.dashboard');         // → "डैशबोर्ड"
 *   QSI18n.switchLanguage('ta');       // Switch to Tamil
 *   QSI18n.applyToDOM();              // Re-translate all [data-i18n] elements
 *
 * @version 1.0.0
 */
const QSI18n = (() => {
    const SUPPORTED_LANGUAGES = {
        en:  { name: 'English',    nativeName: 'English',    flag: '🇬🇧' },
        hi:  { name: 'Hindi',      nativeName: 'हिन्दी',       flag: '🇮🇳' },
        bn:  { name: 'Bengali',    nativeName: 'বাংলা',        flag: '🇮🇳' },
        te:  { name: 'Telugu',     nativeName: 'తెలుగు',       flag: '🇮🇳' },
        mr:  { name: 'Marathi',    nativeName: 'मराठी',        flag: '🇮🇳' },
        ta:  { name: 'Tamil',      nativeName: 'தமிழ்',       flag: '🇮🇳' },
        gu:  { name: 'Gujarati',   nativeName: 'ગુજરાતી',     flag: '🇮🇳' },
        ur:  { name: 'Urdu',       nativeName: 'اردو',         flag: '🇮🇳' },
        kn:  { name: 'Kannada',    nativeName: 'ಕನ್ನಡ',        flag: '🇮🇳' },
        or:  { name: 'Odia',       nativeName: 'ଓଡ଼ିଆ',        flag: '🇮🇳' },
        ml:  { name: 'Malayalam',  nativeName: 'മലയാളം',      flag: '🇮🇳' },
        pa:  { name: 'Punjabi',    nativeName: 'ਪੰਜਾਬੀ',       flag: '🇮🇳' },
        as:  { name: 'Assamese',   nativeName: 'অসমীয়া',      flag: '🇮🇳' },
        mai: { name: 'Maithili',   nativeName: 'मैथिली',       flag: '🇮🇳' },
        sa:  { name: 'Sanskrit',   nativeName: 'संस्कृतम्',    flag: '🇮🇳' },
        kok: { name: 'Konkani',    nativeName: 'कोंकणी',       flag: '🇮🇳' },
        ne:  { name: 'Nepali',     nativeName: 'नेपाली',       flag: '🇮🇳' },
        sd:  { name: 'Sindhi',     nativeName: 'सिन्धी',       flag: '🇮🇳' },
        ks:  { name: 'Kashmiri',   nativeName: 'कश्मीरी',      flag: '🇮🇳' },
        doi: { name: 'Dogri',      nativeName: 'डोगरी',        flag: '🇮🇳' },
        mni: { name: 'Manipuri',   nativeName: 'মৈতৈলোন্',    flag: '🇮🇳' },
        bo:  { name: 'Bodo',       nativeName: 'बोडो',         flag: '🇮🇳' },
        sat: { name: 'Santali',    nativeName: 'ᱥᱟᱱᱛᱟᱲᱤ',    flag: '🇮🇳' }
    };

    let currentLang = 'en';
    let translations = {};
    let fallbackTranslations = {};
    let onLanguageChange = null;

    /**
     * Load translations for a given language code
     */
    async function loadTranslations(langCode) {
        try {
            const response = await fetch(`/js/i18n/${langCode}.json`);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            return await response.json();
        } catch (err) {
            console.warn(`[i18n] Failed to load ${langCode}:`, err.message);
            return {};
        }
    }

    /**
     * Initialize the i18n engine
     * @param {string} lang - Language code (default: 'en')
     * @param {Function} onChange - Callback when language changes
     */
    async function init(lang = 'en', onChange = null) {
        onLanguageChange = onChange;

        // Always load English as fallback
        fallbackTranslations = await loadTranslations('en');

        if (lang !== 'en') {
            translations = await loadTranslations(lang);
        } else {
            translations = fallbackTranslations;
        }

        currentLang = lang;
        applyToDOM();

        // Save preference
        localStorage.setItem('qsdpdp_language', lang);
        console.log(`[i18n] Initialized: ${SUPPORTED_LANGUAGES[lang]?.nativeName || lang}`);
    }

    /**
     * Translate a key
     * @param {string} key - Translation key (e.g., 'nav.dashboard')
     * @param {Object} params - Optional interpolation params
     * @returns {string} Translated string
     */
    function t(key, params = {}) {
        let text = translations[key] || fallbackTranslations[key] || key;

        // Simple interpolation: {{name}} → params.name
        Object.keys(params).forEach(param => {
            text = text.replace(new RegExp(`{{${param}}}`, 'g'), params[param]);
        });

        return text;
    }

    /**
     * Switch to a different language
     * @param {string} langCode - Target language code
     */
    async function switchLanguage(langCode) {
        if (!SUPPORTED_LANGUAGES[langCode]) {
            console.warn(`[i18n] Unsupported language: ${langCode}`);
            return;
        }

        if (langCode === currentLang) return;

        if (langCode === 'en') {
            translations = fallbackTranslations;
        } else {
            translations = await loadTranslations(langCode);
        }

        currentLang = langCode;
        localStorage.setItem('qsdpdp_language', langCode);

        applyToDOM();

        // Update document lang attribute
        document.documentElement.lang = langCode;

        // Trigger callback
        if (onLanguageChange) onLanguageChange(langCode);

        console.log(`[i18n] Switched to: ${SUPPORTED_LANGUAGES[langCode].nativeName}`);
    }

    /**
     * Apply translations to all DOM elements with [data-i18n] attribute
     */
    function applyToDOM() {
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            const translated = t(key);
            if (translated !== key) {
                // Determine where to place text
                if (el.hasAttribute('data-i18n-attr')) {
                    const attr = el.getAttribute('data-i18n-attr');
                    el.setAttribute(attr, translated);
                } else if (el.hasAttribute('placeholder')) {
                    el.placeholder = translated;
                } else {
                    el.textContent = translated;
                }
            }
        });

        // Also update title
        const titleKey = document.querySelector('title[data-i18n]');
        if (titleKey) {
            document.title = t(titleKey.getAttribute('data-i18n'));
        }
    }

    /**
     * Get currently active language
     */
    function getCurrentLanguage() {
        return currentLang;
    }

    /**
     * Get all supported languages
     */
    function getSupportedLanguages() {
        return { ...SUPPORTED_LANGUAGES };
    }

    /**
     * Populate a language dropdown/select element
     * @param {HTMLSelectElement} selectEl - The select element to populate
     */
    function populateLanguageDropdown(selectEl) {
        if (!selectEl) return;
        selectEl.innerHTML = '';
        Object.entries(SUPPORTED_LANGUAGES).forEach(([code, info]) => {
            const option = document.createElement('option');
            option.value = code;
            option.textContent = `${info.flag} ${info.nativeName} (${info.name})`;
            if (code === currentLang) option.selected = true;
            selectEl.appendChild(option);
        });

        selectEl.addEventListener('change', (e) => {
            switchLanguage(e.target.value);
        });
    }

    /**
     * Get saved language from localStorage or browser preference
     */
    function getPreferredLanguage() {
        const saved = localStorage.getItem('qsdpdp_language');
        if (saved && SUPPORTED_LANGUAGES[saved]) return saved;

        // Try browser language
        const browserLang = (navigator.language || '').split('-')[0];
        if (SUPPORTED_LANGUAGES[browserLang]) return browserLang;

        return 'en';
    }

    return {
        init,
        t,
        switchLanguage,
        applyToDOM,
        getCurrentLanguage,
        getSupportedLanguages,
        populateLanguageDropdown,
        getPreferredLanguage,
        SUPPORTED_LANGUAGES
    };
})();
