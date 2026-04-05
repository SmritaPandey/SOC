import { createContext, useContext, useState, useEffect, ReactNode } from 'react'

/* ═══════════════════════════════════════════════════════════════
   APP CONTEXT — Global Sector + Language + i18n
   Selected in Settings → auto-propagates to ALL modules
   ═══════════════════════════════════════════════════════════════ */

// ─── UI Translation Strings ───
const TRANSLATIONS: Record<string, Record<string,string>> = {
  'English': {
    dashboard:'Dashboard',consent:'Consent Management',breach:'Breach Management',rights:'Rights Management',
    siem:'SIEM / SOAR',dlp:'DLP',edr:'EDR / XDR',policy:'Policy Engine',dpia:'DPIA',gap:'Gap Analysis',
    reports:'Reports',settings:'Settings',licensing:'Licensing',payment:'Payment',iam:'IAM / PAM',
    apiHub:'API Hub',certification:'Certification',governance:'Governance',
    active:'Active',pending:'Pending',withdrawn:'Withdrawn',expired:'Expired',total:'Total',
    search:'Search',filter:'Filter',export:'Export',print:'Print',save:'Save',cancel:'Cancel',
    sector:'Sector',language:'Language',organization:'Organization',
    collectConsent:'Collect Consent',viewDetails:'View Details',withdraw:'Withdraw',
    assessmentQuestions:'Assessment Questions',gapAnalysis:'Gap Analysis Report',
    records:'Records',analytics:'Analytics',auditTrail:'Audit Trail',notices:'Notices',
    word:'Word',csv:'CSV',pdf:'PDF',
  },
  'Hindi': {
    dashboard:'डैशबोर्ड',consent:'सहमति प्रबंधन',breach:'उल्लंघन प्रबंधन',rights:'अधिकार प्रबंधन',
    siem:'SIEM / SOAR',dlp:'DLP',edr:'EDR / XDR',policy:'नीति इंजन',dpia:'DPIA',gap:'अंतर विश्लेषण',
    reports:'रिपोर्ट',settings:'सेटिंग्स',licensing:'लाइसेंसिंग',payment:'भुगतान',iam:'IAM / PAM',
    apiHub:'API हब',certification:'प्रमाणन',governance:'शासन',
    active:'सक्रिय',pending:'लंबित',withdrawn:'वापस',expired:'समाप्त',total:'कुल',
    search:'खोजें',filter:'फ़िल्टर',export:'निर्यात',print:'प्रिंट',save:'सहेजें',cancel:'रद्द करें',
    sector:'क्षेत्र',language:'भाषा',organization:'संगठन',
    collectConsent:'सहमति एकत्र करें',viewDetails:'विवरण देखें',withdraw:'वापस लें',
    assessmentQuestions:'मूल्यांकन प्रश्न',gapAnalysis:'अंतर विश्लेषण रिपोर्ट',
    records:'रिकॉर्ड',analytics:'विश्लेषिकी',auditTrail:'ऑडिट ट्रेल',notices:'सूचनाएं',
    word:'Word',csv:'CSV',pdf:'PDF',
  },
  'Bengali': {
    dashboard:'ড্যাশবোর্ড',consent:'সম্মতি ব্যবস্থাপনা',breach:'লঙ্ঘন ব্যবস্থাপনা',rights:'অধিকার ব্যবস্থাপনা',
    siem:'SIEM / SOAR',dlp:'DLP',edr:'EDR / XDR',policy:'নীতি ইঞ্জিন',dpia:'DPIA',gap:'ব্যবধান বিশ্লেষণ',
    reports:'রিপোর্ট',settings:'সেটিংস',licensing:'লাইসেন্সিং',payment:'পেমেন্ট',iam:'IAM / PAM',
    apiHub:'API হাব',certification:'প্রত্যয়ন',governance:'প্রশাসন',
    active:'সক্রিয়',pending:'মুলতুবি',withdrawn:'প্রত্যাহার',expired:'মেয়াদোত্তীর্ণ',total:'মোট',
    search:'অনুসন্ধান',filter:'ফিল্টার',export:'রপ্তানি',print:'প্রিন্ট',save:'সংরক্ষণ',cancel:'বাতিল',
    sector:'সেক্টর',language:'ভাষা',organization:'সংগঠন',
    collectConsent:'সম্মতি সংগ্রহ',viewDetails:'বিস্তারিত দেখুন',withdraw:'প্রত্যাহার',
    assessmentQuestions:'মূল্যায়ন প্রশ্ন',gapAnalysis:'ব্যবধান বিশ্লেষণ রিপোর্ট',
    records:'রেকর্ড',analytics:'বিশ্লেষণ',auditTrail:'অডিট ট্রেল',notices:'বিজ্ঞপ্তি',
    word:'Word',csv:'CSV',pdf:'PDF',
  },
  'Tamil': {
    dashboard:'டாஷ்போர்டு',consent:'ஒப்புதல் மேலாண்மை',breach:'மீறல் மேலாண்மை',rights:'உரிமை மேலாண்மை',
    siem:'SIEM / SOAR',dlp:'DLP',edr:'EDR / XDR',policy:'கொள்கை இயந்திரம்',dpia:'DPIA',gap:'இடைவெளி பகுப்பாய்வு',
    reports:'அறிக்கைகள்',settings:'அமைப்புகள்',licensing:'உரிமம்',payment:'கட்டணம்',iam:'IAM / PAM',
    active:'செயலில்',pending:'நிலுவையில்',withdrawn:'திரும்பப்பெற்றது',expired:'காலாவதி',total:'மொத்தம்',
    search:'தேடு',filter:'வடிகட்டு',export:'ஏற்றுமதி',print:'அச்சிடு',save:'சேமி',cancel:'ரத்துசெய்',
    sector:'துறை',language:'மொழி',organization:'நிறுவனம்',
    collectConsent:'ஒப்புதல் சேகரி',viewDetails:'விவரங்கள் காண',withdraw:'திரும்பப்பெறு',
    records:'பதிவுகள்',analytics:'பகுப்பாய்வு',auditTrail:'தணிக்கை',notices:'அறிவிப்புகள்',
    word:'Word',csv:'CSV',pdf:'PDF',
  },
  'Telugu': {
    dashboard:'డాష్‌బోర్డ్',consent:'సమ్మతి నిర్వహణ',breach:'ఉల్లంఘన నిర్వహణ',rights:'హక్కుల నిర్వహణ',
    active:'సక్రియం',pending:'పెండింగ్',withdrawn:'ఉపసంహరించబడింది',expired:'గడువు ముగిసింది',total:'మొత్తం',
    search:'వెతకండి',filter:'ఫిల్టర్',export:'ఎగుమతి',print:'ప్రింట్',save:'సేవ్',cancel:'రద్దు',
    sector:'రంగం',language:'భాష',records:'రికార్డులు',analytics:'విశ్లేషణ',
    word:'Word',csv:'CSV',pdf:'PDF',
  },
  'Marathi': {
    dashboard:'डॅशबोर्ड',consent:'संमती व्यवस्थापन',breach:'उल्लंघन व्यवस्थापन',rights:'अधिकार व्यवस्थापन',
    active:'सक्रिय',pending:'प्रलंबित',withdrawn:'मागे घेतलेली',expired:'कालबाह्य',total:'एकूण',
    search:'शोधा',filter:'फिल्टर',export:'निर्यात',print:'प्रिंट',save:'जतन करा',cancel:'रद्द करा',
    sector:'क्षेत्र',language:'भाषा',records:'नोंदी',analytics:'विश्लेषण',
    word:'Word',csv:'CSV',pdf:'PDF',
  },
}

interface AppContextType {
  sector: string; setSector: (s: string) => void;
  language: string; setLanguage: (l: string) => void;
  t: (key: string) => string;
}

const AppContext = createContext<AppContextType>({
  sector: 'Banking & Finance', setSector: () => {},
  language: 'English', setLanguage: () => {},
  t: (key: string) => key,
})

export function useAppContext() { return useContext(AppContext) }

export function AppProvider({ children }: { children: ReactNode }) {
  const [sector, setSectorState] = useState(() => localStorage.getItem('qsdpdp_sector') || 'Banking & Finance')
  const [language, setLanguageState] = useState(() => localStorage.getItem('qsdpdp_language') || 'English')

  const setSector = (s: string) => { setSectorState(s); localStorage.setItem('qsdpdp_sector', s) }
  const setLanguage = (l: string) => { setLanguageState(l); localStorage.setItem('qsdpdp_language', l) }

  const t = (key: string): string => {
    const dict = TRANSLATIONS[language] || TRANSLATIONS['English']
    return dict[key] || TRANSLATIONS['English'][key] || key
  }

  return (
    <AppContext.Provider value={{ sector, setSector, language, setLanguage, t }}>
      {children}
    </AppContext.Provider>
  )
}
