/* ═══════════════════════════════════════════════════════════════
   QS-DPDP Enterprise — Sector-Specific Demo Data Generator
   Generates 500+ records per module per sector
   ═══════════════════════════════════════════════════════════════ */

export const SECTORS = [
  'Banking & Finance','Insurance','Healthcare','IT / Software','E-commerce',
  'Telecom','Education','Government','Manufacturing','Energy',
  'Power & Electricity','Defense & DRDO','Media','Transportation','Pharma','Social Media'
] as const

export type Sector = typeof SECTORS[number]

// ─── Indian names for realistic data ───
const FIRST_NAMES = ['Rajesh','Priya','Amit','Sneha','Mohammed','Ananya','Vikram','Lakshmi','Rahul','Deepa','Arjun','Kavita','Suresh','Nisha','Anil','Pooja','Vivek','Sunita','Manish','Ritu','Ravi','Meera','Sanjay','Divya','Ajay','Neha','Prakash','Shalini','Rohit','Aarti','Gaurav','Bhavna','Nikhil','Rekha','Ashish','Swati','Kunal','Pallavi','Sachin','Jyoti','Harish','Preeti','Vinod','Shweta','Mukesh','Rani','Naveen','Geeta','Tarun','Lata']
const LAST_NAMES = ['Kumar','Sharma','Patel','Gupta','Ali','Das','Singh','Iyer','Reddy','Shah','Joshi','Rao','Verma','Mishra','Nair','Chopra','Mehta','Saxena','Tiwari','Kaur','Naik','Sinha','Banerjee','Chauhan','Yadav','Desai','Bhat','Pandey','Malhotra','Kapoor','Agarwal','Khanna','Goyal','Jain','Patil','More','Thakur','Dixit','Taneja','Ahuja']

// ─── Sector-specific consent purposes — User-friendly business descriptions ───
// Each purpose: short label (displayed to user) + description (plain language for data principal)
// + DPDP section (mapped in background) + lawful basis + data types collected
export interface ConsentPurpose {
  label: string;           // e.g. "Account Opening & KYC"
  description: string;     // Plain-language description for data principal
  dpdpSection: string;     // e.g. "Section 6"
  lawfulBasis: string;     // e.g. "Explicit Consent" or "Legal Obligation"
  dataTypes: string[];     // What data is collected
  mandatory: boolean;      // Whether consent is mandatory for service
  retentionPeriod: string; // How long data is retained
}

export const SECTOR_CONSENT_PURPOSES: Record<string, ConsentPurpose[]> = {
  'Banking & Finance': [
    {label:'Account Opening & KYC Verification',description:'We need to verify your identity to open your bank account. This includes collecting your PAN, Aadhaar, photograph, and address proof as required under RBI KYC norms and Prevention of Money Laundering Act.',dpdpSection:'Section 4,6',lawfulBasis:'Legal Obligation + Consent',dataTypes:['Name','Address','PAN','Aadhaar','Photograph','Signature'],mandatory:true,retentionPeriod:'Account lifetime + 5 years'},
    {label:'Loan Processing & Credit Assessment',description:'To evaluate your loan application, we will process your income details, employment information, credit history (via CIBIL/credit bureaus), and existing financial obligations.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Income proof','Bank statements','Credit score','Employment details','Property documents'],mandatory:true,retentionPeriod:'Loan tenure + 3 years'},
    {label:'Credit/Debit Card Processing',description:'We process your transaction data to manage your credit/debit card services including billing, fraud monitoring, reward points, and spending analytics.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity + Consent',dataTypes:['Transaction history','Merchant details','Card details','Billing address'],mandatory:true,retentionPeriod:'Card validity + 7 years'},
    {label:'Digital Banking (UPI/NEFT/RTGS/IMPS)',description:'Your transaction details will be processed for digital payment services. This includes beneficiary details, transaction amounts, and timestamps shared with NPCI/RBI payment networks.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['UPI ID','Account number','Beneficiary details','Transaction metadata'],mandatory:true,retentionPeriod:'10 years (RBI mandate)'},
    {label:'Investment & Wealth Advisory',description:'To provide investment recommendations, we will process your risk profile, financial goals, portfolio holdings, and investment preferences.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Risk profile','Investment history','Net worth','Financial goals'],mandatory:false,retentionPeriod:'Relationship + 5 years'},
    {label:'Marketing & Promotional Offers',description:'We would like to send you information about new banking products, exclusive offers, and financial literacy content via SMS, email, or app notifications. You can opt-out at any time.',dpdpSection:'Section 6,7',lawfulBasis:'Explicit Consent (Optional)',dataTypes:['Contact details','Product preferences','Transaction patterns'],mandatory:false,retentionPeriod:'Until withdrawn'},
    {label:'Insurance Product Cross-Selling',description:'We may share your basic profile with our insurance partners to offer you relevant life, health, or general insurance products. Your data will not be shared without your explicit consent.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent (Optional)',dataTypes:['Name','Age','Contact','Income bracket'],mandatory:false,retentionPeriod:'Until withdrawn'},
    {label:'Fraud Detection & Transaction Monitoring',description:'We monitor transactions for suspicious activity to protect your account from fraud, unauthorized access, and financial crimes as mandated by RBI and PMLA regulations.',dpdpSection:'Section 4,17',lawfulBasis:'Legal Obligation (PMLA)',dataTypes:['Transaction patterns','IP addresses','Device info','Login history'],mandatory:true,retentionPeriod:'5 years (PMLA)'},
    {label:'Aadhaar-based eKYC Authentication',description:'With your consent, we will use your Aadhaar number for electronic Know Your Customer verification through UIDAI. This is an optional, paperless identity verification method.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Aadhaar number','Name','Address','Photograph','Biometrics (OTP-based)'],mandatory:false,retentionPeriod:'5 years'},
    {label:'CIBIL / Credit Bureau Data Sharing',description:'We will share your loan repayment history and credit behavior with CIBIL and other credit information companies as mandated by RBI to maintain your credit record.',dpdpSection:'Section 6,16',lawfulBasis:'Legal Obligation (CICRA)',dataTypes:['Loan details','Repayment history','Default status','Outstanding balances'],mandatory:true,retentionPeriod:'36 months after closure'},
    {label:'Locker Facility & Nomination',description:'For safe deposit locker services, we process your identity details, nominee information, and locker access records.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Identity proof','Nominee details','Access logs','Locker number'],mandatory:true,retentionPeriod:'Locker tenure + 3 years'},
    {label:'Fixed/Recurring Deposit Management',description:'We process your personal and financial details to manage your deposit accounts including maturity, interest, TDS, and auto-renewal preferences.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['PAN','Deposit amount','Maturity details','TDS details','Nomination'],mandatory:true,retentionPeriod:'Deposit maturity + 10 years'},
  ],
  'Healthcare': [
    {label:'Patient Registration & Medical Records',description:'We collect your identity and contact details to create your medical record. This is necessary to provide you healthcare services, maintain treatment history, and ensure continuity of care.',dpdpSection:'Section 4,6',lawfulBasis:'Explicit Consent',dataTypes:['Name','Age','Gender','Contact','Address','Emergency contact','Blood group','Allergies'],mandatory:true,retentionPeriod:'3 years after last visit'},
    {label:'Diagnosis & Treatment Planning',description:'Your medical history, symptoms, test results, and clinical observations will be processed by your treating physician to diagnose your condition and plan your treatment.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Symptoms','Medical history','Examination findings','Previous reports','Medication history'],mandatory:true,retentionPeriod:'Treatment completion + 5 years'},
    {label:'Surgical / Invasive Procedure Consent',description:'Before any surgery or invasive procedure, we need your explicit informed consent covering the procedure, risks, alternatives, expected outcomes, and anesthesia details.',dpdpSection:'Section 6',lawfulBasis:'Explicit Informed Consent',dataTypes:['Procedure details','Risk acknowledgment','Anesthesia consent','Blood transfusion consent'],mandatory:true,retentionPeriod:'Permanent'},
    {label:'Lab Tests & Diagnostic Reports',description:'Your biological samples and diagnostic images will be processed for laboratory testing. Results will be shared with your treating physician and stored in your electronic health record.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Sample details','Test results','Imaging data','Pathology reports'],mandatory:true,retentionPeriod:'5 years'},
    {label:'Insurance Claim & TPA Processing',description:'To process your health insurance claim, we will share your treatment details, bills, and diagnosis information with your insurance company or Third Party Administrator (TPA).',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Treatment summary','Bills','Discharge summary','Diagnosis codes','Policy number'],mandatory:false,retentionPeriod:'Claim settlement + 3 years'},
    {label:'Telemedicine / Video Consultation',description:'Your video consultation will be conducted over an encrypted channel. Session summary and prescription will be stored in your medical record. Recording is optional and requires separate consent.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Video session data','Audio','Prescription','Consultation notes'],mandatory:true,retentionPeriod:'1 year after consultation'},
    {label:'Prescription & Pharmacy Dispensing',description:'Your prescription details will be shared with the pharmacy for medication dispensing. Drug interaction checks and allergy warnings will be automatically generated.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Prescription','Medication list','Allergies','Drug interactions'],mandatory:true,retentionPeriod:'2 years'},
    {label:'Health Checkup & Wellness Program',description:'Your health screening results, lifestyle data, and wellness parameters will be processed to generate your health report and personalized wellness recommendations.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Vital signs','Lab results','BMI','Lifestyle questionnaire'],mandatory:false,retentionPeriod:'3 years'},
    {label:'Clinical Trial Participation',description:'If you participate in a clinical trial, your health data will be collected per the trial protocol approved by the Ethics Committee. Your identity will be pseudonymized in trial reports.',dpdpSection:'Section 4,6,9',lawfulBasis:'Explicit Informed Consent',dataTypes:['Trial-specific data','Adverse events','Lab results','Follow-up data'],mandatory:false,retentionPeriod:'15 years (per ICMR guidelines)'},
    {label:'Mental Health / Psychiatric Records',description:'Your mental health consultation records require the highest level of confidentiality. Access is restricted to your treating psychiatrist/psychologist only.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent (Sensitive)',dataTypes:['Session notes','Psychological assessments','Treatment plans','Medication'],mandatory:true,retentionPeriod:'Patient lifetime'},
    {label:'Organ Donation & Transplant Registry',description:'Your consent to register as an organ donor will be recorded. In case of a match, your medical details will be shared with the transplant coordination center.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Donor card','Blood type','HLA typing','Medical fitness'],mandatory:false,retentionPeriod:'Permanent'},
    {label:'Child / Minor Patient (Guardian Consent)',description:'For patients below 18 years, we require verifiable consent from the parent or legal guardian for all medical procedures and data processing activities.',dpdpSection:'Section 9',lawfulBasis:'Verifiable Guardian Consent',dataTypes:['Minor patient details','Guardian identity','Relationship proof'],mandatory:true,retentionPeriod:'Until child reaches 18 + 3 years'},
  ],
  'Telecom': [
    {label:'SIM Activation & KYC',description:'Your identity details are required for SIM activation as mandated by DoT regulations. This includes Aadhaar-based eKYC or document-based verification.',dpdpSection:'Section 4,6',lawfulBasis:'Legal Obligation (DoT)',dataTypes:['Name','Address','Aadhaar/ID proof','Photograph','Address proof'],mandatory:true,retentionPeriod:'Connection active + 2 years'},
    {label:'Call Data Record (CDR) Processing',description:'Your call records (numbers called, call duration, timestamps) are processed for billing purposes and retained as required by law enforcement regulations.',dpdpSection:'Section 4',lawfulBasis:'Contractual Necessity + Legal Obligation',dataTypes:['Called numbers','Call duration','Timestamps','Tower location'],mandatory:true,retentionPeriod:'2 years (DoT mandate)'},
    {label:'Mobile Data Usage & Billing',description:'Your internet usage data including data consumed, session times, and plan utilization will be processed for billing and plan management.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Data usage','Session logs','Plan details','Billing records'],mandatory:true,retentionPeriod:'3 years'},
    {label:'Location-Based Services',description:'With your consent, we may use your approximate location (tower-based) to provide location-relevant services like nearby store finders, emergency services, and network optimization.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent (Optional)',dataTypes:['Cell tower ID','Approximate location','Timestamps'],mandatory:false,retentionPeriod:'Until withdrawn'},
    {label:'Marketing & Promotional Communications',description:'We would like to send you offers on new plans, recharges, and value-added services via SMS, email, or app. You can register on DND/NDNC to opt-out at any time.',dpdpSection:'Section 6,7',lawfulBasis:'Explicit Consent (Optional)',dataTypes:['Contact details','Usage patterns','Plan preferences'],mandatory:false,retentionPeriod:'Until withdrawn or DND'},
    {label:'Number Portability (MNP)',description:'To process your Mobile Number Portability request, we will share your details with the donor operator and MNPSP as per TRAI regulations.',dpdpSection:'Section 6',lawfulBasis:'Regulatory Requirement (TRAI)',dataTypes:['Mobile number','Subscriber details','UPC code'],mandatory:true,retentionPeriod:'90 days post-porting'},
    {label:'Value-Added Services (VAS)',description:'Subscription to caller tunes, content streaming, gaming, or other VAS requires separate consent. Charges will be applied as per the service terms.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Service subscription','Usage data','Payment details'],mandatory:false,retentionPeriod:'Service subscription period'},
    {label:'Broadband / Fiber Connection',description:'Your address and identity details will be processed for fiber/broadband connection installation, maintenance, and network provisioning.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Installation address','Identity proof','Connection details','Speed plan'],mandatory:true,retentionPeriod:'Connection active + 2 years'},
    {label:'Enterprise / Corporate Solutions',description:'For business accounts, we process organizational details, authorized signatories, employee user data, and usage analytics for enterprise communication solutions.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Organization details','Employee lists','Usage reports','Billing'],mandatory:true,retentionPeriod:'Contract period + 3 years'},
    {label:'Network Quality & Analytics',description:'We collect anonymized network performance data from your device to improve coverage, reduce call drops, and optimize network capacity in your area.',dpdpSection:'Section 4',lawfulBasis:'Legitimate Interest (Anonymized)',dataTypes:['Signal strength','Call quality metrics','Network type','Handset model'],mandatory:false,retentionPeriod:'6 months (anonymized)'},
  ],
  'E-commerce': [
    {label:'Account Registration & Profile',description:'Create your shopping account with basic details. Your profile helps us personalize your experience and manage your orders, addresses, and payment methods.',dpdpSection:'Section 4,6',lawfulBasis:'Contractual Necessity + Consent',dataTypes:['Name','Email','Mobile','Address','Profile photo'],mandatory:true,retentionPeriod:'Account active + 3 years'},
    {label:'Order Processing & Delivery',description:'We process your order details, shipping address, and contact number to fulfill your purchase and coordinate delivery through our logistics partners.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Order details','Shipping address','Contact number','Delivery preferences'],mandatory:true,retentionPeriod:'Order + 5 years (tax records)'},
    {label:'Payment Processing',description:'Your payment details are processed through PCI-DSS compliant payment gateways (Razorpay/PayU/Paytm). We do not store your full card numbers on our servers.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Payment method','Transaction ID','UPI ID','Billing address'],mandatory:true,retentionPeriod:'7 years (tax/audit)'},
    {label:'Personalized Recommendations',description:'Based on your browsing history and past purchases, we suggest products you might like. You can turn off personalization in your account settings.',dpdpSection:'Section 6,7',lawfulBasis:'Explicit Consent (Optional)',dataTypes:['Browse history','Purchase history','Search queries','Category preferences'],mandatory:false,retentionPeriod:'Until withdrawn'},
    {label:'Marketing Emails & Push Notifications',description:'We would like to send you promotional offers, sale alerts, flash deals, and personalized coupons. You can unsubscribe or manage notification preferences anytime.',dpdpSection:'Section 6,7',lawfulBasis:'Explicit Consent (Optional)',dataTypes:['Email','Mobile','Notification token','Preferences'],mandatory:false,retentionPeriod:'Until withdrawn'},
    {label:'Return, Refund & Exchange Processing',description:'For processing returns, refunds, or exchanges, we need your order details, reason for return, and bank account/UPI for refund credit.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Order ID','Return reason','Product photos','Refund details'],mandatory:true,retentionPeriod:'Return + 1 year'},
    {label:'Seller Data Sharing for Fulfillment',description:'Your name and delivery address will be shared with the seller/merchant for order fulfillment. Sellers operate under our marketplace data protection agreement.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Name','Delivery address','Contact (masked)','Order items'],mandatory:true,retentionPeriod:'Delivery + 6 months'},
    {label:'Product Reviews & Ratings',description:'When you write a review, your display name (which you can change), verified purchase badge, and review content will be publicly visible on the product page.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Display name','Review text','Photos/videos','Rating','Purchase verification'],mandatory:false,retentionPeriod:'Permanent (unless deleted)'},
    {label:'Loyalty & Rewards Program',description:'Joining our loyalty program means we track your purchases to award points, tier upgrades, and exclusive benefits. Points history will be maintained for program duration.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Purchase history','Points balance','Tier status','Redemption history'],mandatory:false,retentionPeriod:'Membership + 1 year'},
    {label:'Customer Support & Complaint Resolution',description:'Your support tickets, chat transcripts, and complaint details will be processed to resolve your issues. Calls may be recorded for quality and training purposes.',dpdpSection:'Section 6,14',lawfulBasis:'Contractual Necessity + Consent',dataTypes:['Ticket details','Chat transcripts','Call recordings','Resolution notes'],mandatory:true,retentionPeriod:'Resolution + 1 year'},
  ],
  'Education': [
    {label:'Student Enrollment & Admission',description:'We collect personal and academic details for the admission process including identity verification, academic records, entrance exam scores, and contact information.',dpdpSection:'Section 4,6',lawfulBasis:'Explicit Consent',dataTypes:['Name','DOB','Academic records','ID proof','Photograph','Parent details'],mandatory:true,retentionPeriod:'Enrollment + 10 years'},
    {label:'Examination & Assessment Records',description:'Your answer sheets, marks, grades, internal assessments, and academic performance data will be processed for evaluation and result declaration.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Answer sheets','Marks','Grades','Attendance','Assignment submissions'],mandatory:true,retentionPeriod:'Graduation + 5 years'},
    {label:'Placement Cell & Job Matching',description:'Your resume, academic details, and skills will be shared with recruiting companies that visit our campus. You can opt-out of specific companies.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Resume','CGPA','Skills','Internship records','Interview feedback'],mandatory:false,retentionPeriod:'Graduation + 3 years'},
    {label:'Fee Collection & Financial Aid',description:'Your financial details are processed for fee payment, scholarship eligibility assessment, education loan processing, and financial aid disbursement.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Fee receipts','Bank details','Income certificate','Scholarship documents'],mandatory:true,retentionPeriod:'7 years (audit)'},
    {label:'Online Learning Platform Usage',description:'Your learning activity, course progress, video watch time, quiz scores, and assignment submissions on our LMS will be tracked for academic progress monitoring.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Login times','Course progress','Quiz scores','Assignment uploads','Forum posts'],mandatory:true,retentionPeriod:'Course completion + 2 years'},
    {label:'Alumni Network & Communication',description:'After graduation, we would like to keep you connected through our alumni network for reunions, mentorship programs, and institutional updates.',dpdpSection:'Section 6,7',lawfulBasis:'Explicit Consent (Optional)',dataTypes:['Name','Email','Company','Designation','Graduation year'],mandatory:false,retentionPeriod:'Until withdrawn'},
    {label:'Library & Digital Resource Access',description:'Your borrowing history, digital resource downloads, and research database access will be recorded for library management and copyright compliance.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Borrowed books','Access logs','Download history','Fine records'],mandatory:true,retentionPeriod:'Enrollment period + 1 year'},
    {label:'Hostel & Accommodation Management',description:'Your room allocation, biometric access logs, visitor records, and mess preferences will be maintained for hostel administration and safety.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity + Consent',dataTypes:['Room allocation','Biometric logs','Visitor records','Emergency contact'],mandatory:true,retentionPeriod:'Stay period + 1 year'},
    {label:'Parent/Guardian Communication',description:'We share academic progress, attendance alerts, fee reminders, and institutional announcements with registered parents/guardians of enrolled students.',dpdpSection:'Section 6,9',lawfulBasis:'Explicit Consent',dataTypes:['Parent name','Contact','Student progress','Attendance','Fee status'],mandatory:true,retentionPeriod:'Enrollment period'},
    {label:'Research Data & Publication',description:'If you participate in research projects, your contribution data will be processed per the research ethics committee guidelines. Published data will be anonymized.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Research data','Survey responses','Experiment records','Consent forms'],mandatory:false,retentionPeriod:'Research completion + 5 years'},
  ],
  'IT / Software': [
    {label:'Employee Onboarding & HR Records',description:'Your personal details, educational qualifications, previous employment, and identity documents are processed for employment verification, payroll setup, and statutory compliance.',dpdpSection:'Section 4,6',lawfulBasis:'Contractual Necessity + Legal Obligation',dataTypes:['Name','Address','PAN','Aadhaar','Bank account','Education certificates','Experience letters'],mandatory:true,retentionPeriod:'Employment + 7 years'},
    {label:'Client Data Processing (DPA)',description:'As a data processor for our clients, we process end-user personal data strictly within the scope defined by the Data Processing Agreement (DPA) with each client.',dpdpSection:'Section 8(2)',lawfulBasis:'Data Processing Agreement',dataTypes:['Client-defined data scope','Processing logs','Security audit trails'],mandatory:true,retentionPeriod:'Contract period + as per DPA'},
    {label:'Background Verification (BGV)',description:'With your consent, we will verify your educational qualifications, previous employment, criminal record, and address through authorized verification agencies.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Education records','Previous employer details','Address verification','Criminal check'],mandatory:true,retentionPeriod:'Employment + 1 year'},
    {label:'Remote Work & Device Monitoring',description:'Company-issued devices may have endpoint monitoring for security purposes including DLP, access logs, and application usage. Personal devices are not monitored.',dpdpSection:'Section 6',lawfulBasis:'Legitimate Interest + Consent',dataTypes:['Device inventory','Application logs','Network access','Location (office/remote)'],mandatory:true,retentionPeriod:'Employment period'},
    {label:'Performance Review & Appraisal',description:'Your work performance, project contributions, peer feedback, and manager assessments will be processed for annual appraisal and career development planning.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['KRA scores','Peer reviews','Manager feedback','Project metrics','Training completed'],mandatory:true,retentionPeriod:'Employment + 3 years'},
    {label:'Training & Certification Records',description:'Your training attendance, certification scores, and skill development records are maintained for compliance, project staffing, and career growth tracking.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Training records','Certification scores','Skill matrix','Learning path'],mandatory:true,retentionPeriod:'Employment period + 2 years'},
    {label:'Third-Party Vendor Data Sharing',description:'In outsourcing arrangements, limited employee data may be shared with authorized vendors for payroll processing, insurance, or travel management. All vendors sign DPAs.',dpdpSection:'Section 6,8(2)',lawfulBasis:'Explicit Consent + DPA',dataTypes:['Name','Employee ID','Contact','Insurance details','Travel documents'],mandatory:false,retentionPeriod:'Vendor contract period'},
    {label:'Exit Process & Data Retention',description:'Upon separation, your employment records, exit interview data, and final settlement details will be processed. You may request deletion of non-statutory data.',dpdpSection:'Section 6,8(7)',lawfulBasis:'Legal Obligation + Consent',dataTypes:['Resignation letter','Exit feedback','Settlement details','Experience letter'],mandatory:true,retentionPeriod:'Statutory minimum (7 years for tax/PF)'},
  ],
  'Government': [
    {label:'Citizen Identity Registration (Aadhaar)',description:'Your biometric and demographic data is collected for Aadhaar enrollment and identity verification as mandated under the Aadhaar Act, 2016.',dpdpSection:'Section 4,17',lawfulBasis:'Legal Obligation (Aadhaar Act)',dataTypes:['Name','DOB','Address','Biometrics','Photograph','Mobile'],mandatory:true,retentionPeriod:'Lifetime'},
    {label:'Tax Filing & Assessment',description:'Your income, deductions, and tax payment details are processed for income tax return filing and assessment under the Income Tax Act, 1961.',dpdpSection:'Section 4,17',lawfulBasis:'Legal Obligation (IT Act)',dataTypes:['PAN','Income details','Deductions','TDS certificates','Bank statements'],mandatory:true,retentionPeriod:'7 years (IT Act)'},
    {label:'Subsidy & Welfare Distribution (DBT)',description:'Your identity and bank details are used for Direct Benefit Transfer of government subsidies, pensions, and welfare scheme amounts to your bank account.',dpdpSection:'Section 4,17',lawfulBasis:'Legal Obligation',dataTypes:['Aadhaar','Bank account','Scheme enrollment','Eligibility data'],mandatory:true,retentionPeriod:'Scheme period + 5 years'},
    {label:'Public Grievance Redressal',description:'Your complaint details, supporting documents, and contact information are processed to address your grievance through the official grievance redressal mechanism.',dpdpSection:'Section 14',lawfulBasis:'Statutory Obligation',dataTypes:['Complaint details','Supporting documents','Contact info','Resolution history'],mandatory:true,retentionPeriod:'Resolution + 3 years'},
    {label:'Passport & Visa Processing',description:'Your identity, address, family details, and travel history are processed for passport issuance and visa application under the Passports Act, 1967.',dpdpSection:'Section 4,17',lawfulBasis:'Legal Obligation',dataTypes:['Identity documents','Address proof','Family details','Travel history','Police verification'],mandatory:true,retentionPeriod:'Passport validity + 5 years'},
    {label:'Voter Registration & Electoral Roll',description:'Your name, address, age, and photograph are collected for voter ID issuance and inclusion in the electoral roll under the Representation of People Act.',dpdpSection:'Section 4,17',lawfulBasis:'Legal Obligation',dataTypes:['Name','Address','DOB','Photograph','EPIC number'],mandatory:false,retentionPeriod:'Until deletion request'},
  ],
  'Insurance': [
    {label:'Policy Application & Underwriting',description:'Your personal details, health history, income, and lifestyle information are assessed to evaluate your insurance application and determine premium.',dpdpSection:'Section 4,6',lawfulBasis:'Contractual Necessity + Consent',dataTypes:['Health history','Income proof','Lifestyle questionnaire','Medical reports','Age proof'],mandatory:true,retentionPeriod:'Policy term + 5 years'},
    {label:'Claims Processing & Settlement',description:'Your claim documents, hospital bills, FIR (if applicable), and supporting evidence will be processed for claim assessment, verification, and settlement.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Claim form','Hospital bills','Medical reports','FIR','Survey report'],mandatory:true,retentionPeriod:'Claim + 8 years'},
    {label:'Premium Calculation & Renewal',description:'Your age, health status, claim history, and risk factors are processed to calculate premiums and send renewal reminders before your policy expires.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Premium history','Claim history','Risk profile','Renewal dates'],mandatory:true,retentionPeriod:'Policy active + 3 years'},
    {label:'Health Declaration & Medical Examination',description:'Your declaration of existing illnesses, medical examinations, and health test reports are required for risk assessment and policy approval.',dpdpSection:'Section 6',lawfulBasis:'Explicit Consent',dataTypes:['Health declaration','Medical examination reports','Pre-existing conditions','Prescribed medications'],mandatory:true,retentionPeriod:'Policy term + 10 years'},
    {label:'Nominee & Beneficiary Registration',description:'Your nominated beneficiary details are recorded for claim settlement in case of the policyholder\'s demise or incapacity.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity + Consent',dataTypes:['Nominee name','Relationship','ID proof','Contact details','Share percentage'],mandatory:true,retentionPeriod:'Policy term + settlement'},
    {label:'Agent/Intermediary Data Processing',description:'Your details as an insurance agent/intermediary are processed for licensing, commission calculation, and regulatory compliance under IRDAI guidelines.',dpdpSection:'Section 4,6',lawfulBasis:'Contractual + Legal Obligation',dataTypes:['Agent license','Training records','Commission data','KYC','IRDAI registration'],mandatory:true,retentionPeriod:'Agent active + 5 years'},
  ],
  'Manufacturing': [
    {label:'Employee Safety & Health Records',description:'Your safety training records, health checkup results, and incident reports are maintained for workplace safety compliance under the Factories Act.',dpdpSection:'Section 4,6',lawfulBasis:'Legal Obligation (Factories Act)',dataTypes:['Safety training','Health records','Incident reports','PPE records'],mandatory:true,retentionPeriod:'Employment + 10 years'},
    {label:'Quality Control & Traceability',description:'Product batch, operator details, and inspection records are maintained for quality assurance and regulatory traceability requirements.',dpdpSection:'Section 4',lawfulBasis:'Legal Obligation + Contractual',dataTypes:['Operator ID','Batch records','Inspection data','Test results'],mandatory:true,retentionPeriod:'Product lifecycle + 10 years'},
    {label:'Vendor & Supply Chain Management',description:'Vendor contact details, performance records, and compliance certificates are processed for supply chain management and procurement.',dpdpSection:'Section 6',lawfulBasis:'Contractual Necessity',dataTypes:['Vendor contacts','Performance scores','Compliance certificates','Order history'],mandatory:true,retentionPeriod:'Contract + 3 years'},
    {label:'CCTV & Access Control',description:'Your biometric access logs and CCTV footage at manufacturing premises are recorded for security, safety compliance, and incident investigation.',dpdpSection:'Section 6',lawfulBasis:'Legitimate Interest + Legal Obligation',dataTypes:['Biometric logs','CCTV footage','Entry/exit times','Visitor records'],mandatory:true,retentionPeriod:'90 days (CCTV), 1 year (access logs)'},
  ],
}

// Backward compatibility: flatten labels for legacy code
export const SECTOR_PURPOSES: Record<string, string[]> = {}
Object.entries(SECTOR_CONSENT_PURPOSES).forEach(([sector, purposes]) => {
  SECTOR_PURPOSES[sector] = purposes.map(p => p.label)
})
// Fill missing sectors
SECTORS.forEach(s => {
  if (!SECTOR_PURPOSES[s]) SECTOR_PURPOSES[s] = SECTOR_PURPOSES['Banking & Finance']
})


// ─── Sector-specific policies — Comprehensive Indian Laws, Regulations, ISO Standards ───
const P = (id:string,title:string,category:string,dpdpSection:string,controls:string[],score:number,status:string) => ({id,title,category,dpdpSection,controls,complianceScore:score,status})
export const SECTOR_POLICIES: Record<string, {id:string,title:string,category:string,dpdpSection:string,controls:string[],complianceScore:number,status:string}[]> = {
  'Banking & Finance': [
    // DPDP Act 2023
    P('BF-001','DPDP Act — Consent Management Policy','CONSENT','Section 6,7',['Granular consent','Withdrawal mechanism','Consent receipts','Purpose limitation'],95,'ACTIVE'),
    P('BF-002','DPDP Act — Data Principal Rights Policy','DATA_PROTECTION','Section 11-14',['Access requests','Correction workflow','Erasure process','Grievance SLA 30d'],92,'ACTIVE'),
    P('BF-003','DPDP Act — Notice & Transparency Policy','DATA_PROTECTION','Section 5',['Collection notice','Processing disclosure','Retention periods','Multi-language notice'],90,'ACTIVE'),
    P('BF-004','DPDP Act — Data Breach Notification Policy','BREACH','Section 8(6)',['72h DPBI notification','6h CERT-IN report','Customer notification','Forensic capture'],94,'ACTIVE'),
    P('BF-005','DPDP Act — Children Data Protection Policy','DATA_PROTECTION','Section 9',['Age verification','Verifiable parental consent','No behavioral tracking','No targeted ads'],85,'ACTIVE'),
    P('BF-006','DPDP Act — Cross-Border Transfer Policy','CROSS_BORDER','Section 16',['Government whitelist check','Adequacy assessment','Contractual safeguards','Transfer log'],78,'REVIEW'),
    P('BF-007','DPDP Act — Data Fiduciary Obligations','DATA_PROTECTION','Section 8',['Purpose limitation','Data minimization','Storage limitation','Accuracy maintenance'],91,'ACTIVE'),
    P('BF-008','DPDP Act — Significant Data Fiduciary Policy','DATA_PROTECTION','Section 10',['DPO appointment','DPIA process','Annual audit','Algorithm transparency'],82,'ACTIVE'),
    // RBI Regulations
    P('BF-009','RBI — KYC / AML Policy (Master Direction 2016)','DATA_PROTECTION','RBI MD KYC',['Aadhaar eKYC','PAN validation','Video KYC','Risk categorization','Periodic re-KYC'],95,'ACTIVE'),
    P('BF-010','RBI — Master Direction on Digital Payment Security','ACCESS_CONTROL','RBI DPS 2021',['Two-factor auth','Transaction limits','Encryption at rest/transit','Device binding'],93,'ACTIVE'),
    P('BF-011','RBI — Customer Data Localization (Storage of Payment Data)','DATA_PROTECTION','RBI Circular Apr 2018',['Data stored in India','Mirror copy prohibited','End-to-end audit','Six-month compliance'],88,'ACTIVE'),
    P('BF-012','RBI — Outsourcing Guidelines for Banks','DATA_PROTECTION','RBI/2006-07/274',['Vendor due diligence','SLA with DPA clauses','Sub-contracting approval','Exit management'],84,'ACTIVE'),
    P('BF-013','RBI — Cyber Security Framework for Banks','ACCESS_CONTROL','RBI Circular 2016',['SOC setup','Vulnerability assessment','Red team testing','Board reporting'],90,'ACTIVE'),
    P('BF-014','RBI — Integrated Ombudsman Scheme Policy','RETENTION','RBI IOS 2021',['30-day resolution SLA','Escalation matrix','Compensation framework','Digital complaint portal'],86,'ACTIVE'),
    // PMLA & Financial Laws
    P('BF-015','PMLA — Anti-Money Laundering Data Policy','DATA_PROTECTION','PMLA 2002 Sec 12',['STR filing','CTR reporting','Record keeping 5yr','PEP screening'],90,'ACTIVE'),
    P('BF-016','SARFAESI Act — Asset Recovery Data Policy','DATA_PROTECTION','SARFAESI 2002',['Borrower notice','Auction data','Recovery records','DRT filing'],82,'ACTIVE'),
    P('BF-017','Negotiable Instruments Act — Cheque Data Policy','RETENTION','NI Act 1881',['CTS image retention','Cheque return memo','NACH mandate data','ECS records'],80,'ACTIVE'),
    P('BF-018','CICRA — Credit Information Policy','DATA_PROTECTION','CICRA 2005',['CIBIL data sharing','Credit report consent','Dispute resolution','36-month retention'],85,'ACTIVE'),
    // SEBI & Capital Markets
    P('BF-019','SEBI — Investor Data Protection Policy','DATA_PROTECTION','SEBI Act 1992',['Demat KYC','Trading data privacy','Insider trading monitoring','Client segregation'],87,'ACTIVE'),
    P('BF-020','SEBI — Cyber Security & Resilience Framework','ACCESS_CONTROL','SEBI/HO/MRD/2018',['VAPT quarterly','SOC monitoring','Incident reporting','DR testing'],89,'ACTIVE'),
    // IT Act & General
    P('BF-021','IT Act 2000 — Information Security Policy','ACCESS_CONTROL','IT Act Sec 43A, 72A',['Reasonable security practices','ISO 27001 alignment','Compensation for breach','Criminal liability'],91,'ACTIVE'),
    P('BF-022','IT (SPDI Rules) 2011 — Sensitive Data Policy','DATA_PROTECTION','SPDI Rules',['Written consent for SPDI','Purpose disclosure','Grievance officer','Data retention policy'],88,'ACTIVE'),
    // ISO Standards
    P('BF-023','ISO 27001:2022 — ISMS Policy','ACCESS_CONTROL','ISO 27001',['Risk assessment','Statement of Applicability','Internal audit','Management review','Continual improvement'],92,'ACTIVE'),
    P('BF-024','ISO 27701:2019 — Privacy Management (PIMS)','DATA_PROTECTION','ISO 27701',['PII inventory','Processing records','Privacy risk assessment','Third-party assessment'],85,'ACTIVE'),
    P('BF-025','PCI-DSS v4.0 — Payment Card Data Policy','ACCESS_CONTROL','PCI-DSS',['Network segmentation','Encryption','Access control','Vulnerability management','Penetration testing'],90,'ACTIVE'),
    P('BF-026','ISO 22301 — Business Continuity Policy','DATA_PROTECTION','ISO 22301',['BIA','DR plan','RTO/RPO targets','Annual BCP drill','Crisis communication'],87,'ACTIVE'),
    P('BF-027','Indian Evidence Act — Digital Evidence Policy','RETENTION','IEA Sec 65B',['Hash integrity','Chain of custody','Certified copy','Electronic record admissibility'],83,'ACTIVE'),
  ],
  'Healthcare': [
    // DPDP Act
    P('HC-001','DPDP Act — Patient Consent Management','CONSENT','Section 6,7',['Informed consent','Purpose-specific','Withdrawal anytime','Consent receipts'],91,'ACTIVE'),
    P('HC-002','DPDP Act — Patient Rights Policy','DATA_PROTECTION','Section 11-14',['Access to records','Correction request','Erasure workflow','Grievance 30d SLA'],88,'ACTIVE'),
    P('HC-003','DPDP Act — Health Data Breach Policy','BREACH','Section 8(6)',['72h DPBI','6h CERT-IN','Patient notification','PHI impact assessment'],93,'ACTIVE'),
    P('HC-004','DPDP Act — Children Health Data Policy','DATA_PROTECTION','Section 9',['Guardian consent','Age verification','No profiling minors','Pediatric data handling'],79,'ACTIVE'),
    P('HC-005','DPDP Act — Cross-Border Health Data Transfer','CROSS_BORDER','Section 16',['Transfer assessment','Contractual safeguards','Government whitelist','Audit trail'],71,'DRAFT'),
    // Indian Healthcare Laws
    P('HC-006','Clinical Establishments Act 2010 — Records Policy','RETENTION','CEA 2010',['Medical record maintenance','Minimum standards','Registration compliance','Inspection readiness'],85,'ACTIVE'),
    P('HC-007','PCPNDT Act 1994 — Prenatal Diagnostic Data','DATA_PROTECTION','PCPNDT Act',['No sex determination records','Consent Form F','Monthly reporting','Record sealing 2yr'],90,'ACTIVE'),
    P('HC-008','MTP Act 2021 — Abortion Records Privacy','DATA_PROTECTION','MTP Act',['Confidentiality mandatory','No disclosure without consent','Opinion of RMP','Record retention'],88,'ACTIVE'),
    P('HC-009','Mental Healthcare Act 2017 — Psychiatric Records','DATA_PROTECTION','MHA 2017',['Advance directive','Nominated representative','Mental Health Board','Restricted access'],82,'ACTIVE'),
    P('HC-010','Drugs & Cosmetics Act — Prescription Data','RETENTION','D&C Act 1940',['Schedule H/H1 records','E-prescription standards','Pharmacy dispensing logs','Adverse drug reporting'],84,'ACTIVE'),
    P('HC-011','ICMR — Biomedical Research Ethics Policy','DATA_PROTECTION','ICMR Guidelines 2017',['Ethics committee approval','Informed consent','Data anonymization','Results publication'],86,'ACTIVE'),
    P('HC-012','ICMR — National Ethical Guidelines for AI in Health','DATA_PROTECTION','ICMR AI Guidelines',['Algorithm transparency','Bias assessment','Clinical validation','Human oversight'],75,'REVIEW'),
    P('HC-013','NMC — Telemedicine Practice Guidelines','CONSENT','NMC 2020',['Patient identification','Consent for teleconsult','Prescription format','Record keeping 3yr'],82,'ACTIVE'),
    P('HC-014','Epidemic Diseases Act — Surveillance Data','DATA_PROTECTION','EDA 1897, Amd 2020',['Outbreak data','Contact tracing','Anonymized reporting','Purpose limitation'],80,'ACTIVE'),
    P('HC-015','ABHA — Ayushman Bharat Health Account Policy','DATA_PROTECTION','ABDM Framework',['ABHA ID generation','Health record linking','Consent manager','PHR standards'],78,'ACTIVE'),
    P('HC-016','Organ Transplant Act — Donor/Recipient Privacy','DATA_PROTECTION','THOA 1994',['Donor anonymity','Authorization committee','Transplant registry','Cross-match data'],83,'ACTIVE'),
    // ISO Standards
    P('HC-017','ISO 27001:2022 — Hospital ISMS','ACCESS_CONTROL','ISO 27001',['Risk assessment','Access control','Incident management','Business continuity'],92,'ACTIVE'),
    P('HC-018','ISO 27799:2016 — Health Informatics Security','ACCESS_CONTROL','ISO 27799',['PHI classification','Break-glass access','Audit logging','De-identification'],87,'ACTIVE'),
    P('HC-019','ISO 27701:2019 — Health Privacy Management','DATA_PROTECTION','ISO 27701',['PII lifecycle','Patient consent records','Processing purpose register','Third-party DPA'],85,'ACTIVE'),
    P('HC-020','ISO 13485 — Medical Device Data Policy','DATA_PROTECTION','ISO 13485',['Device traceability','Post-market surveillance','CDSCO registration','UDI compliance'],81,'ACTIVE'),
    P('HC-021','NABH — Accreditation Data Standards','DATA_PROTECTION','NABH Standards',['EMR standards','Patient safety reporting','Clinical audit','Quality indicators'],84,'ACTIVE'),
    P('HC-022','IT Act 2000 — e-Health Records Security','ACCESS_CONTROL','IT Act Sec 43A',['Reasonable security','Body corporate liability','Compensation framework','Criminal prosecution'],91,'ACTIVE'),
    P('HC-023','Bio-Medical Waste Rules — Data Tracking','RETENTION','BMW Rules 2016',['Waste manifest','Treatment records','Annual report SPCB','GPS tracking data'],76,'ACTIVE'),
    P('HC-024','Indian Evidence Act — Medical Records Admissibility','RETENTION','IEA Sec 65B',['Electronic record authentication','Doctor testimony','Certified copies','Chain of custody'],80,'ACTIVE'),
  ],
  'Telecom': [
    P('TE-001','DPDP Act — Subscriber Consent Policy','CONSENT','Section 6,7',['SIM activation consent','CDR processing notice','Marketing opt-in','Consent withdrawal'],90,'ACTIVE'),
    P('TE-002','DPDP Act — Subscriber Rights Policy','DATA_PROTECTION','Section 11-14',['Access to CDRs','Data correction','Account deletion','Grievance 30d'],88,'ACTIVE'),
    P('TE-003','DoT — Unified License Data Privacy Clause','DATA_PROTECTION','UL Clause 39',['Lawful interception','Data localisation','CDR retention 2yr','Government access'],92,'ACTIVE'),
    P('TE-004','TRAI — Unsolicited Communication Regulations','CONSENT','TCCCPR 2018',['DND/NDNC registration','Consent-based messaging','Scrubbing compliance','Penalty framework'],85,'ACTIVE'),
    P('TE-005','TRAI — Quality of Service Regulations','RETENTION','QoS Regulations',['Network performance data','Call drop reporting','Speed test records','Monthly submission'],82,'ACTIVE'),
    P('TE-006','TRAI — MNP Regulations Data Policy','DATA_PROTECTION','MNP Regulations',['Porting data handling','Subscriber consent','UPC generation','90-day retention'],80,'ACTIVE'),
    P('TE-007','Telecom Security Policy — CERT-IN Integration','ACCESS_CONTROL','DoT TSP',['6h incident reporting','SOC operations','VAPT quarterly','Threat intelligence'],89,'ACTIVE'),
    P('TE-008','IT Act 2000 — Telecom Data Security','ACCESS_CONTROL','IT Act Sec 43A, 69',['Interception procedures','Monitoring rules','Decryption orders','Intermediary liability'],87,'ACTIVE'),
    P('TE-009','Indian Telegraph Act — Communication Data','DATA_PROTECTION','Telegraph Act 1885',['Licensed operations','Government interception','Emergency provisions','Cable records'],84,'ACTIVE'),
    P('TE-010','ISO 27001 — Telecom ISMS','ACCESS_CONTROL','ISO 27001',['Risk assessment','Network security','Incident response','Audit cycle'],91,'ACTIVE'),
    P('TE-011','ISO 27011 — Telecom-specific ISMS','ACCESS_CONTROL','ISO 27011',['Subscriber privacy','Network element security','Interconnection security','CDR protection'],86,'ACTIVE'),
    P('TE-012','5G/IoT Data Governance Policy','DATA_PROTECTION','DoT 5G Guidelines',['Edge data processing','IoT device identity','Network slicing privacy','URLLC data handling'],72,'DRAFT'),
  ],
  'IT / Software': [
    P('IT-001','DPDP Act — Employee Data Policy','DATA_PROTECTION','Section 4,6',['HR consent','Purpose limitation','Exit data deletion','Cross-border employee data'],90,'ACTIVE'),
    P('IT-002','DPDP Act — Client Data Processing Agreement','DATA_PROTECTION','Section 8(2)',['DPA with clients','Sub-processor approval','Breach notification','Audit rights'],92,'ACTIVE'),
    P('IT-003','IT Act 2000 — Information Security Policy','ACCESS_CONTROL','IT Act Sec 43A',['Reasonable security practices','Body corporate liability','ISO 27001 alignment','Incident reporting'],91,'ACTIVE'),
    P('IT-004','IT (SPDI Rules) 2011 — Sensitive Data Handling','DATA_PROTECTION','SPDI Rules',['Written consent','Purpose limitation','Grievance officer','Data retention limits'],88,'ACTIVE'),
    P('IT-005','IT (Intermediary Guidelines) 2021 — Platform Policy','DATA_PROTECTION','IT Rules 2021',['Content moderation','Grievance officer','Compliance officer','Monthly transparency report'],85,'ACTIVE'),
    P('IT-006','CERT-IN Directions 2022 — Incident Reporting','BREACH','CERT-IN 2022',['6-hour incident report','Log retention 180 days','VPN KYC','Clock synchronization NTP'],90,'ACTIVE'),
    P('IT-007','SEZ Act — Special Zone Data Policy','DATA_PROTECTION','SEZ Act 2005',['Export obligations','Data in SEZ','Customs data','STPI compliance'],78,'ACTIVE'),
    P('IT-008','ISO 27001:2022 — ISMS','ACCESS_CONTROL','ISO 27001',['Risk assessment','SOA','Internal audit','Management review','Continual improvement'],93,'ACTIVE'),
    P('IT-009','ISO 27701:2019 — Privacy Management','DATA_PROTECTION','ISO 27701',['PII inventory','Processing register','Privacy risk assessment','Third-party DPA'],86,'ACTIVE'),
    P('IT-010','ISO 27017 — Cloud Security Controls','ACCESS_CONTROL','ISO 27017',['Shared responsibility','Cloud access control','Virtual machine security','Tenant isolation'],84,'ACTIVE'),
    P('IT-011','ISO 27018 — Cloud PII Protection','DATA_PROTECTION','ISO 27018',['PII in cloud','Data portability','Transparency','Sub-processor control'],82,'ACTIVE'),
    P('IT-012','SOC 2 Type II — Trust Service Criteria','ACCESS_CONTROL','SOC 2',['Security','Availability','Processing integrity','Confidentiality','Privacy'],88,'ACTIVE'),
    P('IT-013','CMMI — Process Maturity Policy','DATA_PROTECTION','CMMI Level 5',['Process standardization','Quantitative management','Continuous improvement','Defect prevention'],80,'ACTIVE'),
    P('IT-014','GDPR — EU Client Data Processing','CROSS_BORDER','GDPR Art 28',['EU adequacy','BCR','SCC','DPO for EU data'],83,'ACTIVE'),
    P('IT-015','Shops & Establishments Act — Employee Records','RETENTION','State S&E Acts',['Working hours','Leave records','Wage register','Statutory benefits'],75,'ACTIVE'),
  ],
  'Insurance': [
    P('IN-001','DPDP Act — Policyholder Consent Policy','CONSENT','Section 6,7',['Application consent','Claims processing consent','Third-party sharing consent','Marketing opt-in'],90,'ACTIVE'),
    P('IN-002','DPDP Act — Policyholder Rights','DATA_PROTECTION','Section 11-14',['Policy data access','Correction requests','Nominee updates','Grievance resolution'],87,'ACTIVE'),
    P('IN-003','IRDAI — Protection of Policyholders Interest Regulations','DATA_PROTECTION','IRDAI PPHI 2017',['Proposal form disclosure','Claims settlement SLA','Premium transparency','Dispute resolution'],91,'ACTIVE'),
    P('IN-004','IRDAI — Outsourcing Activities Regulation','DATA_PROTECTION','IRDAI Outsourcing 2017',['Vendor risk assessment','Data access control','Sub-contracting approval','Monthly review'],84,'ACTIVE'),
    P('IN-005','IRDAI — Information & Cyber Security Guidelines','ACCESS_CONTROL','IRDAI Cyber 2023',['SOC operations','Vulnerability management','Incident response','Board reporting'],88,'ACTIVE'),
    P('IN-006','IRDAI — Health Insurance Regulations','DATA_PROTECTION','IRDAI Health 2020',['Pre-existing disease disclosure','Waiting period clarity','Cashless procedure data','TPA data sharing'],86,'ACTIVE'),
    P('IN-007','Insurance Act 1938 — Data Retention Policy','RETENTION','Insurance Act',['Proposal retention','Claims records 8yr','Agent records','Audit trail'],82,'ACTIVE'),
    P('IN-008','PMLA — Insurance AML Policy','DATA_PROTECTION','PMLA 2002',['Customer due diligence','STR filing','PEP screening','Record keeping 5yr'],85,'ACTIVE'),
    P('IN-009','Motor Vehicles Act — Motor Insurance Data','RETENTION','MV Act 1988',['Vehicle registration data','Claim intimation records','Survey reports','Third-party liability'],80,'ACTIVE'),
    P('IN-010','ISO 27001 — Insurance ISMS','ACCESS_CONTROL','ISO 27001',['Risk assessment','Access control','Incident management','Business continuity'],91,'ACTIVE'),
    P('IN-011','ISO 27701 — Insurance Privacy Management','DATA_PROTECTION','ISO 27701',['PHI handling','Nominee data protection','Cross-border transfers','Third-party assessment'],84,'ACTIVE'),
  ],
  'E-commerce': [
    P('EC-001','DPDP Act — Consumer Consent Policy','CONSENT','Section 6,7',['Registration consent','Marketing opt-in','Third-party sharing','Cookie consent'],89,'ACTIVE'),
    P('EC-002','DPDP Act — Consumer Rights Policy','DATA_PROTECTION','Section 11-14',['Account data access','Order history export','Account deletion','Grievance portal'],86,'ACTIVE'),
    P('EC-003','Consumer Protection Act 2019 — E-Commerce Rules','DATA_PROTECTION','CPA 2019',['Product information disclosure','Return/refund policy','Grievance officer','No misleading ads'],90,'ACTIVE'),
    P('EC-004','Consumer Protection (E-Commerce) Rules 2020','DATA_PROTECTION','E-Com Rules 2020',['Seller information display','Country of origin','Cancellation rights','Complaint mechanism'],87,'ACTIVE'),
    P('EC-005','IT (Intermediary Guidelines) 2021 — Marketplace','DATA_PROTECTION','IT Rules 2021',['Grievance officer 24h ack','Content takedown 36h','Monthly compliance report','User verification'],85,'ACTIVE'),
    P('EC-006','FSSAI — Food Delivery Data Policy','RETENTION','FSSAI Act 2006',['Restaurant license display','Nutrition info','Allergen data','Cold chain tracking'],78,'ACTIVE'),
    P('EC-007','Legal Metrology Act — Product Data Policy','RETENTION','LM Act 2009',['Weight/measure disclosure','MRP display','Country of origin','Best before date'],80,'ACTIVE'),
    P('EC-008','PCI-DSS v4.0 — Payment Card Security','ACCESS_CONTROL','PCI-DSS',['Card data tokenization','PII masking','Network segmentation','Quarterly ASV scan'],90,'ACTIVE'),
    P('EC-009','GST — Invoice Data Retention Policy','RETENTION','CGST Act 2017',['E-invoice generation','72-month retention','E-way bill data','ITC reconciliation'],82,'ACTIVE'),
    P('EC-010','ISO 27001 — E-Commerce ISMS','ACCESS_CONTROL','ISO 27001',['Risk assessment','Web application security','DDoS protection','Secure SDLC'],88,'ACTIVE'),
    P('EC-011','FDI Policy — Foreign E-Commerce Compliance','DATA_PROTECTION','DPIIT Press Note 2 (2018)',['Marketplace model compliance','FDI limit tracking','Inventory model prohibition','Vendor reporting to DPIIT'],78,'ACTIVE'),
    P('EC-012','Competition Commission — Anti-Competitive Data Practices','DATA_PROTECTION','Competition Act Sec 3-4',['Self-preferencing audit','Deep discount monitoring','Search neutrality','Platform-to-business fairness'],75,'REVIEW'),
    P('EC-013','ONDC — Open Network Data Integration','DATA_PROTECTION','ONDC Protocol Specs',['Buyer data portability','Seller onboarding data','Logistics integration','Interoperability standards'],70,'DRAFT'),
    P('EC-014','Logistics & Delivery Data Policy','RETENTION','MoRTH / India Post',['Delivery tracking data','Address verification','Cash on delivery records','Last-mile partner data sharing'],80,'ACTIVE'),
    P('EC-015','ISO 27701 — E-Commerce Privacy Management','DATA_PROTECTION','ISO 27701',['Customer PII lifecycle','Cross-border transfers','Third-party processor DPA','Cookie consent management'],83,'ACTIVE'),
  ],
  'Education': [
    P('ED-001','DPDP Act — Student Data Consent Policy','CONSENT','Section 6,9',['Student/guardian consent','Minor data protection','Purpose limitation','Marketing restrictions'],88,'ACTIVE'),
    P('ED-002','DPDP Act — Student Rights Policy','DATA_PROTECTION','Section 11-14',['Academic record access','Data correction','Transfer certificate data','Grievance mechanism'],85,'ACTIVE'),
    P('ED-003','RTE Act 2009 — Student Records Policy','DATA_PROTECTION','RTE Act',['Enrollment records','Attendance tracking','No detention data','TC issuance'],82,'ACTIVE'),
    P('ED-004','UGC Regulations — University Data Standards','RETENTION','UGC Act 1956',['Academic records retention','Convocation data','Faculty records','Research data'],80,'ACTIVE'),
    P('ED-005','AICTE — Technical Education Data Policy','RETENTION','AICTE Act 1987',['Approval records','Faculty qualification data','Student enrollment','Placement data'],78,'ACTIVE'),
    P('ED-006','NEP 2020 — Academic Bank of Credits Data','DATA_PROTECTION','NEP 2020',['ABC registration','Credit transfer records','Multi-entry/exit data','DigiLocker integration'],75,'REVIEW'),
    P('ED-007','POCSO Act — Child Safety in Schools','DATA_PROTECTION','POCSO 2012',['Incident reporting','Background verification staff','CCTV policy','Confidentiality of victim'],90,'ACTIVE'),
    P('ED-008','ISO 27001 — Educational Institution ISMS','ACCESS_CONTROL','ISO 27001',['Student data security','LMS security','Exam integrity','Research data protection'],86,'ACTIVE'),
    P('ED-009','ISO 21001 — Educational Organization Management','DATA_PROTECTION','ISO 21001',['Learner-centric approach','Special needs data','Feedback management','Performance monitoring'],79,'ACTIVE'),
    P('ED-010','NAAC / NBA — Accreditation Data Policy','RETENTION','NAAC / NBA Framework',['SSR data submission','DVV records','IIQA documents','Student survey data','Peer review records'],82,'ACTIVE'),
    P('ED-011','NIRF — Ranking Data Governance','RETENTION','MHRD NIRF Parameters',['TLR parameter data','Research publication data','Graduation outcome data','Outreach/inclusivity data','Faculty qualification records'],78,'ACTIVE'),
    P('ED-012','CBSE / ICSE / State Boards — Examination Data Security','ACCESS_CONTROL','Board Examination Rules',['Question paper security','Marksheet data integrity','Result publication protocol','Re-evaluation records','Admit card data'],90,'ACTIVE'),
    P('ED-013','EdTech / Online Learning — Student Digital Privacy','DATA_PROTECTION','DPDP + IT Act',['Video proctoring consent','Screen recording policy','Learning analytics opt-in','Student engagement tracking','LMS data retention'],80,'ACTIVE'),
    P('ED-014','DigiLocker — Academic Document Integration','DATA_PROTECTION','DigiLocker API Standards',['ABC credit deposit','Verified document issuance','NAD integration','eKYC for student verification'],76,'ACTIVE'),
    P('ED-015','Shops & Establishment Act — Non-Teaching Staff Data','RETENTION','State S&E Acts',['Employee records','Salary registers','Leave records','PF/ESI compliance','Contract staff data'],75,'ACTIVE'),
  ],
  'Government': [
    P('GV-001','DPDP Act — Citizen Data Policy','DATA_PROTECTION','Section 4,17',['Lawful processing','State instrumentality exemption','Purpose limitation','Transparency'],90,'ACTIVE'),
    P('GV-002','DPDP Act — Government Exemptions Policy','DATA_PROTECTION','Section 17',['National security','Public order','Sovereignty','Prevention of offenses'],85,'ACTIVE'),
    P('GV-003','Aadhaar Act 2016 — Identity Data Policy','DATA_PROTECTION','Aadhaar Act',['Biometric data protection','Authentication only','No tracking','UIDAI guidelines'],92,'ACTIVE'),
    P('GV-004','RTI Act 2005 — Information Disclosure Policy','DATA_PROTECTION','RTI Act',['Proactive disclosure','Third-party info protection','PIO obligations','Appeal process'],88,'ACTIVE'),
    P('GV-005','Official Secrets Act — Classified Data','ACCESS_CONTROL','OSA 1923',['Classification levels','Need-to-know access','Declassification schedule','Espionage prevention'],90,'ACTIVE'),
    P('GV-006','e-Governance Standards — MeitY','DATA_PROTECTION','e-Gov Standards',['GIGW compliance','Accessibility standards','Open API policy','Data.gov.in publishing'],82,'ACTIVE'),
    P('GV-007','CERT-IN — Government Cyber Security Policy','ACCESS_CONTROL','NCSP 2013',['6h incident reporting','SOC mandate','VAPT quarterly','Cyber crisis management'],89,'ACTIVE'),
    P('GV-008','IT Act Sec 69 — Interception & Monitoring Policy','ACCESS_CONTROL','IT Act Sec 69',['Authorized interception','Decryption orders','Review committee','Record destruction 6mo'],87,'ACTIVE'),
    P('GV-009','ISO 27001 — Government ISMS','ACCESS_CONTROL','ISO 27001',['Risk assessment','Access control','Audit logging','Incident management'],91,'ACTIVE'),
    P('GV-010','General Financial Rules — Government Data Retention','RETENTION','GFR 2017',['Expenditure records 10yr','Contract data','Tender documents','GeM procurement records','Audit trail'],82,'ACTIVE'),
    P('GV-011','Central Vigilance Commission — Anti-Corruption Data','ACCESS_CONTROL','CVC Guidelines',['Complaint registers','Vigilance case files','Annual property returns','Disproportionate asset records'],85,'ACTIVE'),
    P('GV-012','State IT Policies — Data Center & Cloud Governance','ACCESS_CONTROL','MeitY Cloud Policy',['GI Cloud (MeghRaj)','Data center tier requirements','Data localisation','BCP for state portals'],80,'ACTIVE'),
    P('GV-013','IndEA — India Enterprise Architecture Framework','DATA_PROTECTION','IndEA 2.0',['Reference architecture compliance','Service delivery data','Interoperability standards','API gateway security'],78,'ACTIVE'),
    P('GV-014','National Data Sharing & Accessibility Policy','DATA_PROTECTION','NDSAP 2012',['Open data principles','Shareable/non-shareable classification','Contributory data sharing','Licensing framework'],76,'ACTIVE'),
  ],
  'Manufacturing': [
    P('MF-001','DPDP Act — Employee & Vendor Data Policy','DATA_PROTECTION','Section 4,6',['Employee consent','Vendor KYC','Purpose limitation','Data retention limits'],88,'ACTIVE'),
    P('MF-002','Factories Act 1948 — Worker Safety Records','RETENTION','Factories Act',['Health records','Safety training','Accident registers','Inspection reports'],90,'ACTIVE'),
    P('MF-003','Environmental Protection Act — Compliance Data','RETENTION','EPA 1986',['Emission data','Effluent records','Hazardous waste manifest','SPCB returns'],85,'ACTIVE'),
    P('MF-004','BIS Standards — Product Quality Data','RETENTION','BIS Act 2016',['Test certificates','ISI marking records','Quality audit data','Consumer complaint log'],82,'ACTIVE'),
    P('MF-005','ESIC Act — Employee Insurance Data','DATA_PROTECTION','ESIC Act 1948',['Wage records','Contribution data','Medical records','Benefit claims'],80,'ACTIVE'),
    P('MF-006','EPF Act — Provident Fund Data','DATA_PROTECTION','EPF Act 1952',['UAN records','Contribution history','Transfer data','Settlement records'],82,'ACTIVE'),
    P('MF-007','ISO 9001 — Quality Management Data','RETENTION','ISO 9001',['Quality records','NCR tracking','CAPA records','Customer feedback'],87,'ACTIVE'),
    P('MF-008','ISO 14001 — Environmental Management Data','RETENTION','ISO 14001',['Environmental aspects','Legal compliance','Monitoring records','Emergency records'],84,'ACTIVE'),
    P('MF-009','ISO 45001 — OH&S Management Data','DATA_PROTECTION','ISO 45001',['Incident records','Health surveillance','Risk assessment','Worker consultation'],86,'ACTIVE'),
    P('MF-010','ISO 27001 — Manufacturing ISMS','ACCESS_CONTROL','ISO 27001',['OT/IT security','SCADA protection','Supply chain security','IP protection'],88,'ACTIVE'),
    P('MF-011','Industrial Disputes Act — Worker Data Governance','RETENTION','ID Act 1947',['Strike/lockout records','Lay-off/retrenchment data','Conciliation records','Tribunal proceedings'],80,'ACTIVE'),
    P('MF-012','PLI Scheme — Production-Linked Incentive Data','RETENTION','DPIIT PLI Guidelines',['Production threshold records','Investment data','Employment generation data','Quarterly compliance reports'],78,'ACTIVE'),
    P('MF-013','IIoT / OT Security — Smart Factory Data','ACCESS_CONTROL','IEC 62443',['OT network segmentation','PLC/SCADA protection','Edge computing data','Digital twin data privacy'],84,'ACTIVE'),
    P('MF-014','Intellectual Property — Trade Secret & Patent Data','ACCESS_CONTROL','Patents Act / Copyright Act',['Trade secret classification','Patent filing records','Design registration data','Non-disclosure agreements'],86,'ACTIVE'),
    P('MF-015','Contract Labour Act — Outsourced Workforce Data','DATA_PROTECTION','CLRA 1970',['Contractor registration','Worker muster rolls','Wage payment records','Welfare fund data'],77,'ACTIVE'),
  ],
  'Energy': [
    P('EN-001','DPDP Act — Energy Consumer Data Policy','DATA_PROTECTION','Section 4,6',['Consumer consent','Usage processing','Billing transparency','Data principal rights'],86,'ACTIVE'),
    P('EN-002','PNGRB Act — Gas Distribution Data','RETENTION','PNGRB Act 2006',['Pipeline data','Safety records','CGD consumer data','Emergency response'],80,'ACTIVE'),
    P('EN-003','Atomic Energy Act — Nuclear Data Security','ACCESS_CONTROL','AE Act 1962',['Classified nuclear data','Personnel security clearance','Physical protection','AERB compliance'],92,'ACTIVE'),
    P('EN-004','Petroleum Act — Oil & Gas Data','RETENTION','Petroleum Act 1934',['Exploration data','Production records','Safety inspections','Environmental clearance'],82,'ACTIVE'),
    P('EN-005','Coal Mines Regulations — Mining Data','RETENTION','CMR 2017',['Safety records','Worker health data','Production logs','Accident registers'],80,'ACTIVE'),
    P('EN-006','ISO 27001 — Energy Sector ISMS','ACCESS_CONTROL','ISO 27001',['OT/ICS security','Network isolation','Incident response','Supply chain security'],88,'ACTIVE'),
    P('EN-007','ISO 50001 — Energy Management Data','RETENTION','ISO 50001',['Energy performance data','Baseline calculation','Monitoring records','Improvement records'],82,'ACTIVE'),
    P('EN-008','ISO 14001 — Environmental Management','RETENTION','ISO 14001',['Environmental impact','Emission data','Compliance monitoring','Waste management'],84,'ACTIVE'),
  ],
  'Power & Electricity': [
    // DPDP Act
    P('PW-001','DPDP Act — Consumer Meter Data Consent','CONSENT','Section 6,7',['Smart meter consent','AMI data processing notice','Billing data consent','Marketing opt-in'],88,'ACTIVE'),
    P('PW-002','DPDP Act — Consumer Rights (Power Sector)','DATA_PROTECTION','Section 11-14',['Billing data access','Meter data correction','Account deletion','Grievance 30d SLA'],85,'ACTIVE'),
    // Electricity Act & Regulations
    P('PW-003','Electricity Act 2003 — Consumer Data Protection','DATA_PROTECTION','Electricity Act 2003',['Consumer database protection','Billing records retention','Meter reading data','Connection records','Supply quality data'],90,'ACTIVE'),
    P('PW-004','Electricity Act 2003 — Licensee Obligations (Sec 55-62)','RETENTION','EA 2003 Sec 55-62',['Consumer complaint register','Electricity supply records','Metering standards','Billing disputes log'],87,'ACTIVE'),
    P('PW-005','CEA — Technical Standards Data Policy','RETENTION','CEA (Measures) Regulations',['Grid code compliance','Protection relay data','Equipment standards','Safety clearance records'],84,'ACTIVE'),
    P('PW-006','CERC — Tariff & Commercial Data Policy','RETENTION','CERC Regulations',['Tariff order data','Power purchase agreements','Open access records','Renewable energy certificates (REC)','ABT settlement data'],82,'ACTIVE'),
    P('PW-007','SERC — State Distribution Data Policy','RETENTION','SERC (Supply Code)',['Consumer category data','Load shedding records','AT&C loss data','Distribution transformer data','Revenue collection'],80,'ACTIVE'),
    P('PW-008','NLDC/POSOCO — Grid Operation Data','ACCESS_CONTROL','Grid Code Regulations',['Real-time frequency data','SCADA dispatch records','Inter-state energy accounting','Emergency protocols','Load dispatch schedules'],89,'ACTIVE'),
    P('PW-009','Smart Grid / AMI Data Governance Policy','DATA_PROTECTION','NSGM Guidelines',['Smart meter data privacy','15-min interval consumption','Demand response data','Prepaid meter records','Net metering data'],78,'REVIEW'),
    P('PW-010','Renewable Energy Data Policy (Solar/Wind)','RETENTION','MNRE Guidelines',['RE generation data','RPO compliance records','GBI/subsidy records','Rooftop solar data','Green energy certificate'],80,'ACTIVE'),
    P('PW-011','Power System SCADA/ICS Security Policy','ACCESS_CONTROL','CEI/CERT-IN',['SCADA/EMS protection','RTU security','IEC 61850 compliance','Substation automation security','6h incident reporting'],91,'ACTIVE'),
    P('PW-012','National Electricity Policy — Data Standards','DATA_PROTECTION','NEP 2005',['Electrification data','Rural/urban supply data','Power quality monitoring','Consumer satisfaction surveys'],75,'ACTIVE'),
    P('PW-013','Electricity (Rights of Consumers) Rules 2020','DATA_PROTECTION','Consumer Rights Rules',['24h connection SLA','Complaint resolution 48h','Compensation framework','Prosumer data handling','Net metering settlement'],86,'ACTIVE'),
    P('PW-014','Power Finance Corporation — Loan Data Policy','DATA_PROTECTION','PFC Guidelines',['DISCOM financial data','UDAY scheme records','Loss reduction targets','Capital expenditure data'],78,'ACTIVE'),
    P('PW-015','IS/IEC 62351 — Power System Communication Security','ACCESS_CONTROL','IS/IEC 62351',['SCADA protocol security','DNP3 authentication','IEC 61850 GOOSE security','Key management','Certificate authority'],87,'ACTIVE'),
    P('PW-016','ISO 27001 — Power Sector ISMS','ACCESS_CONTROL','ISO 27001',['OT/IT convergence security','Grid SCADA protection','Substation network security','Incident response','DR/BCP for grid operations'],90,'ACTIVE'),
    P('PW-017','ISO 55001 — Power Asset Management Data','RETENTION','ISO 55001',['Asset lifecycle data','Condition monitoring records','Preventive maintenance logs','Asset performance analytics'],82,'ACTIVE'),
  ],
  'Defense & DRDO': [
    // DPDP Act
    P('DF-001','DPDP Act — Defense Exemption Policy','DATA_PROTECTION','Section 17(2)(a)',['National security exemption','State instrumentality','Sovereignty protection','Controlled disclosure'],90,'ACTIVE'),
    P('DF-002','DPDP Act — Defense Personnel Data Policy','DATA_PROTECTION','Section 4,6',['Service records consent','Pension data handling','Veterans data protection','Family welfare data'],85,'ACTIVE'),
    // Official Secrets & National Security
    P('DF-003','Official Secrets Act 1923 — Classified Information','ACCESS_CONTROL','OSA 1923',['SECRET/TOP SECRET/CONFIDENTIAL handling','Need-to-know basis','Declassification schedule','Vetting & polygraph','Unauthorized disclosure penalties'],95,'ACTIVE'),
    P('DF-004','National Security Act 1980 — Security Data','ACCESS_CONTROL','NSA 1980',['Threat intelligence classification','Detention records','Security assessment data','Border security data'],92,'ACTIVE'),
    // Armed Forces
    P('DF-005','Army Act 1950 / Navy Act 1957 / Air Force Act 1950','ACCESS_CONTROL','Armed Forces Acts',['Service records','Court martial data','Operational deployment records','Casualty casualty data','Medal & citation records'],93,'ACTIVE'),
    P('DF-006','Armed Forces (Special Powers) Act — Operations Data','ACCESS_CONTROL','AFSPA',['Operational data classification','Force deployment records','Counter-insurgency data','Communication interception'],90,'ACTIVE'),
    P('DF-007','CDS — Joint Theatre Command Data Policy','ACCESS_CONTROL','CDS Directives',['Tri-service data sharing','Joint operations data','Integrated theatre command records','Inter-service communication security'],88,'ACTIVE'),
    // DRDO Specific
    P('DF-008','DRDO — Research Lab Data Classification','ACCESS_CONTROL','DRDO Classification Policy',['Project classification (Secret/Top Secret)','52 lab data governance','Technology readiness levels','Prototype data protection','Test range data'],94,'ACTIVE'),
    P('DF-009','DRDO — Intellectual Property & Patent Policy','DATA_PROTECTION','DRDO IPR Policy',['Patent filing records','Technology transfer data','Licensing agreements','Innovation disclosure','Royalty management'],87,'ACTIVE'),
    P('DF-010','DRDO — SCOMET Export Control Data','ACCESS_CONTROL','SCOMET List (DGFT)',['Dual-use technology export control','End-user certificate data','MTCR/Wassenaar compliance','Export license records','Technology denial lists'],91,'ACTIVE'),
    P('DF-011','DRDO — Strategic Program Data (Missile/Nuclear)','ACCESS_CONTROL','AEC Act + DRDO',['Strategic program classification','Nuclear capable system data','Missile program records','Space weapon data','Deterrence posture data'],96,'ACTIVE'),
    P('DF-012','DRDO — Vendor & Make-in-India Defence Data','DATA_PROTECTION','DPP 2020 / DAP 2020',['Vendor clearance records','Indigenization data','Technology transfer agreements','Offset obligation tracking','FDI compliance in defence'],85,'ACTIVE'),
    // Defence Procurement & MoD
    P('DF-013','Defence Acquisition Procedure 2020 — Procurement Data','DATA_PROTECTION','DAP 2020',['RFP/RFI data protection','Trial evaluation records','Contract award data','Life cycle cost data','Warranty & AMC records'],84,'ACTIVE'),
    P('DF-014','MoD — Defence Budget & Financial Data','RETENTION','MoD Financial Rules',['Defence expenditure records','Capital acquisition data','Revenue budget tracking','CAG audit compliance','Parliamentary committee data'],82,'ACTIVE'),
    P('DF-015','Ordnance Factory Board — Production Data','ACCESS_CONTROL','OFB/New DPSUs',['Ammunition production records','Quality test data','Proof range records','Inventory management','Supply chain tracking'],88,'ACTIVE'),
    // Space & Dual-Use
    P('DF-016','ISRO — Space Program Data Security','ACCESS_CONTROL','ISRO Security Policy',['Satellite data classification','Launch vehicle records','Remote sensing data policy','Space situational awareness','IRNSS/NavIC data'],90,'ACTIVE'),
    // Standards
    P('DF-017','ISO 27001 — Defence ISMS (Air-Gapped Networks)','ACCESS_CONTROL','ISO 27001',['Military-grade PQC encryption','Air-gapped network security','Personnel vetting L1-L4','Secure communications','TEMPEST compliance'],93,'ACTIVE'),
    P('DF-018','ISO 15408 — Common Criteria for Defence IT','ACCESS_CONTROL','ISO 15408 / CC',['Security target definition','EAL 4+ assessment','Protection profile','NDcPP compliance','Certification & accreditation'],88,'ACTIVE'),
  ],
  'Media': [
    P('MD-001','DPDP Act — Viewer/Reader Data Policy','CONSENT','Section 6,7',['Subscription consent','Content personalization','Ad targeting consent','Data principal rights'],86,'ACTIVE'),
    P('MD-002','IT (Intermediary Guidelines) 2021 — Digital Media','DATA_PROTECTION','IT Rules 2021 Part III',['Content classification','Grievance officer','Self-regulatory body','Compliance report'],88,'ACTIVE'),
    P('MD-003','Cable Television Networks Act 1995 — Subscriber Data','DATA_PROTECTION','CTN Act',['Subscriber records','Addressable system data','STB pairing','Carriage data'],80,'ACTIVE'),
    P('MD-004','Cinematograph Act 1952 — Content Classification Data','RETENTION','Cinematograph Act',['CBFC certificate records','Classification data','Cuts/modifications log','Exhibition records'],78,'ACTIVE'),
    P('MD-005','Press Council Act — Journalist Data','DATA_PROTECTION','PC Act 1978',['Source confidentiality','Journalist accreditation','Complaint records','Ethical guidelines'],82,'ACTIVE'),
    P('MD-006','Copyright Act 1957 — Digital Rights Management','RETENTION','Copyright Act',['DRM metadata','Licensing records','Infringement tracking','Royalty data'],84,'ACTIVE'),
    P('MD-007','ISO 27001 — Media ISMS','ACCESS_CONTROL','ISO 27001',['Content delivery security','DRM protection','User data security','Ad platform security'],87,'ACTIVE'),
    P('MD-008','PIB / DAVP — Government Advertising Data','RETENTION','PIB Empanelment Rules',['Empanelment data','Rate card records','DAVP billing','Circulation/viewership data'],76,'ACTIVE'),
    P('MD-009','Broadcasting Content Complaints Council','DATA_PROTECTION','BCCC / NBF Code of Ethics',['Viewer complaint records','Content review data','Fine/penalty records','Self-regulation compliance'],78,'ACTIVE'),
    P('MD-010','OTT / Digital Media — IT Rules Compliance','DATA_PROTECTION','IT Rules Part III',['Content classification (U/A/A)','Self-regulatory body membership','Grievance appellate committee','Content description'],84,'ACTIVE'),
    P('MD-011','ASCI — Advertising Standards Data Policy','RETENTION','ASCI Code',['Ad complaint tracking','CCC decisions','Influencer disclosure records','Surrogate advertising monitoring'],79,'ACTIVE'),
    P('MD-012','ISO 27701 — Media Privacy Management','DATA_PROTECTION','ISO 27701',['Subscriber PII lifecycle','Content recommendation consent','Viewer analytics privacy','Third-party ad tracking DPA'],83,'ACTIVE'),
  ],
  'Transportation': [
    P('TR-001','DPDP Act — Passenger Data Policy','CONSENT','Section 6,7',['Booking consent','Travel data processing','Loyalty program consent','Marketing opt-in'],86,'ACTIVE'),
    P('TR-002','Motor Vehicles Act 2019 — Vehicle Data','DATA_PROTECTION','MV Act Amd 2019',['Registration data','Driving license','Fitness certificate','E-challan data'],88,'ACTIVE'),
    P('TR-003','DGCA — Aviation Passenger Data','DATA_PROTECTION','DGCA CAR',['PNR data retention','No-fly list','Security screening','Incident reports'],85,'ACTIVE'),
    P('TR-004','Railways Act 1989 — Passenger Data','DATA_PROTECTION','Railways Act',['IRCTC booking data','Aadhaar linking','Cancellation records','Refund data'],82,'ACTIVE'),
    P('TR-005','Merchant Shipping Act 1958 — Maritime Data','RETENTION','MS Act',['Crew records','Cargo manifest','Port clearance','Safety inspection'],80,'ACTIVE'),
    P('TR-006','Metro Railways Act 2002 — Transit Data','DATA_PROTECTION','Metro Act',['Smart card data','CCTV footage','Access logs','Ridership analytics'],79,'ACTIVE'),
    P('TR-007','FASTag/ETC — Toll Data Policy','DATA_PROTECTION','NHAI Guidelines',['Vehicle tracking','Toll transaction data','Wallet data','Location data'],83,'ACTIVE'),
    P('TR-008','ISO 27001 — Transport ISMS','ACCESS_CONTROL','ISO 27001',['Booking system security','Fleet management data','Passenger data protection','Payment security'],87,'ACTIVE'),
    P('TR-009','NHAI — Highway Toll & Traffic Data','RETENTION','NHAI Guidelines',['Traffic density data','FASTag transaction records','Vehicle classification data','Revenue reconciliation'],80,'ACTIVE'),
    P('TR-010','Electric Vehicle — Charging & Battery Data','DATA_PROTECTION','MoP / FAME II',['EV charging session data','Battery telemetry','Station location data','Subsidy eligibility records'],76,'ACTIVE'),
    P('TR-011','DGCA — Drone / RPAS Data Governance','DATA_PROTECTION','Drone Rules 2021',['UIN/UAOP registration','Flight log data','No-drone zone compliance','Payload data policy'],82,'ACTIVE'),
    P('TR-012','AAI — Airport Operations Data Security','ACCESS_CONTROL','AAI / BCAS Rules',['Passenger manifest','CCTV surveillance policy','Access control biometric','Cargo screening data'],85,'ACTIVE'),
    P('TR-013','ISO 27701 — Transport Privacy Management','DATA_PROTECTION','ISO 27701',['Passenger PII lifecycle','Booking data retention','Location tracking consent','Third-party aggregator DPA'],83,'ACTIVE'),
  ],
  'Pharma': [
    P('PH-001','DPDP Act — Patient Trial Data Policy','CONSENT','Section 6,9',['Clinical trial consent','Adverse event reporting','Data subject rights','Cross-border transfer'],88,'ACTIVE'),
    P('PH-002','Drugs & Cosmetics Act 1940 — Drug Data','RETENTION','D&C Act',['Drug registration data','Batch records','Stability data','GMP compliance records'],90,'ACTIVE'),
    P('PH-003','CDSCO — Clinical Trial Data Rules 2019','DATA_PROTECTION','CT Rules 2019',['Ethics committee approval','Informed consent','SAE reporting','Bioavailability data'],87,'ACTIVE'),
    P('PH-004','Pharmacy Act 1948 — Pharmacist Records','RETENTION','Pharmacy Act',['Registration records','Qualification data','Continuing education','Disciplinary records'],80,'ACTIVE'),
    P('PH-005','NDPS Act — Narcotic Drug Records','RETENTION','NDPS Act 1985',['Controlled substance records','License data','Movement registers','Destruction certificates'],92,'ACTIVE'),
    P('PH-006','National Pharmaceutical Pricing Authority — Price Data','RETENTION','DPCO 2013',['MRP ceiling data','Price revision records','Sales audit data','NLEM compliance'],82,'ACTIVE'),
    P('PH-007','Pharmacovigilance Programme of India','BREACH','PvPI',['ADR reporting','Signal detection','PSUR data','Risk management plans'],85,'ACTIVE'),
    P('PH-008','WHO-GMP / Schedule M — Manufacturing Data','RETENTION','Schedule M',['Batch manufacturing records','Validation data','Deviation records','CAPA tracking'],88,'ACTIVE'),
    P('PH-009','ISO 27001 — Pharma ISMS','ACCESS_CONTROL','ISO 27001',['R&D data protection','IP security','Trial data encryption','Supply chain integrity'],86,'ACTIVE'),
    P('PH-010','ICH-GCP — Good Clinical Practice','DATA_PROTECTION','ICH E6(R2)',['Sponsor obligations','Investigator records','Monitoring reports','Data integrity'],84,'ACTIVE'),
  ],
  'Social Media': [
    P('SM-001','DPDP Act — User Consent & Processing','CONSENT','Section 6,7',['Registration consent','Content personalization','Behavioral profiling opt-in','Third-party data sharing'],85,'ACTIVE'),
    P('SM-002','DPDP Act — User Rights (Access/Erasure/Portability)','DATA_PROTECTION','Section 11-14',['Download your data','Account deletion','Data portability','Grievance resolution 30d'],82,'ACTIVE'),
    P('SM-003','DPDP Act — Children on Social Media','DATA_PROTECTION','Section 9',['Age gate 18 years','No behavioral monitoring','Parental consent','No targeted ads to minors'],78,'REVIEW'),
    P('SM-004','IT (Intermediary Guidelines) 2021 — SSMI Compliance','DATA_PROTECTION','IT Rules Part II',['Grievance officer India','Chief compliance officer','Monthly report','First originator tracing'],90,'ACTIVE'),
    P('SM-005','IT Act Sec 69A — Content Blocking Orders','ACCESS_CONTROL','IT Act Sec 69A',['Government takedown orders','Blocking procedure','Review committee','Emergency blocking'],88,'ACTIVE'),
    P('SM-006','IT Act Sec 79 — Safe Harbor & Due Diligence','DATA_PROTECTION','IT Act Sec 79',['Content moderation policy','User notice','Takedown SLA 36h','Transparency report'],86,'ACTIVE'),
    P('SM-007','Election Commission — Political Ad Policy','DATA_PROTECTION','ECI Guidelines',['Political ad labeling','Spending disclosure','Dark post restrictions','Campaign period rules'],80,'ACTIVE'),
    P('SM-008','Representation of People Act — Election Data','RETENTION','RPA 1951',['Political advertising records','Paid news tracking','Model code compliance','Voter data protection'],79,'ACTIVE'),
    P('SM-009','ISO 27001 — Social Media ISMS','ACCESS_CONTROL','ISO 27001',['Platform security','API security','Content delivery','User data encryption'],87,'ACTIVE'),
    P('SM-010','Deepfake / AI-Generated Content Policy','DATA_PROTECTION','IT Act + MeitY Advisory',['AI content labeling','Deepfake detection','Synthetic media disclosure','User reporting mechanism'],75,'REVIEW'),
    P('SM-011','Creator Economy — Influencer Data Policy','DATA_PROTECTION','ASCI / CCPA Guidelines',['Influencer disclosure compliance','Paid partnership labeling','Audience data sharing','Tax (TDS) reporting data'],78,'ACTIVE'),
    P('SM-012','Algorithmic Transparency & Bias Policy','DATA_PROTECTION','MeitY AI Ethics Framework',['Algorithm audit records','Content ranking transparency','Bias detection reports','User opt-out from profiling'],72,'DRAFT'),
    P('SM-013','Fake News / Misinformation — Fact-Check Policy','DATA_PROTECTION','IT Rules Amd 2023',['Fact-check unit compliance','Government-flagged content','User appeal mechanism','Content review records'],80,'ACTIVE'),
    P('SM-014','ISO 27701 — Social Media Privacy Management','DATA_PROTECTION','ISO 27701',['User PII lifecycle','Cross-platform data sharing','Ad targeting consent','Third-party app DPA'],83,'ACTIVE'),
  ],
}

// ═══════════════════════════════════════════════════════════════
// ORGANIZATIONAL POLICIES — Common to ALL sectors/organizations
// Covers: HR, IT, Finance, Legal, Operations, Quality, Communications, Safety
// These are auto-merged with sector-specific regulatory policies
// ═══════════════════════════════════════════════════════════════
const ORGANIZATIONAL_POLICIES = [
  // ──── GOVERNANCE & CORPORATE ────
  P('ORG-GOV-001','Corporate Governance Policy','GOVERNANCE','Companies Act 2013 Sec 134-166',['Board composition & independence','Director duties & liabilities','Related party transactions','KMP appointment','Shareholder rights'],90,'ACTIVE'),
  P('ORG-GOV-002','Delegation of Authority (DoA) Policy','GOVERNANCE','Companies Act / Board Resolution',['Financial approval matrix','Signing authority levels','Capital expenditure limits','Hiring authority tiers','Vendor commitment limits'],88,'ACTIVE'),
  P('ORG-GOV-003','Conflict of Interest Policy','GOVERNANCE','Companies Act Sec 184',['Annual disclosure by directors','Related party register','Gift & hospitality limits','Outside directorship approval','Employee moonlighting rules'],85,'ACTIVE'),
  P('ORG-GOV-004','Anti-Bribery & Anti-Corruption Policy','GOVERNANCE','Prevention of Corruption Act 1988',['Zero tolerance statement','Facilitation payment prohibition','Gift register & limits (₹5,000)','Political contribution ban','Third-party due diligence'],92,'ACTIVE'),
  P('ORG-GOV-005','Whistleblower / Vigil Mechanism Policy','GOVERNANCE','Companies Act Sec 177(9)',['Anonymous reporting channel','Protection from retaliation','Investigation procedure','Audit committee oversight','Quarterly reporting to Board'],90,'ACTIVE'),
  P('ORG-GOV-006','Code of Business Ethics','GOVERNANCE','Board Charter',['Ethical conduct standards','Fair dealing principles','Confidentiality obligations','Compliance with laws','Disciplinary consequences'],88,'ACTIVE'),
  P('ORG-GOV-007','Enterprise Risk Management (ERM) Policy','GOVERNANCE','ISO 31000:2018 / COSO ERM',['Risk appetite statement','Risk register maintenance','Risk assessment methodology','Risk reporting to Board','Emerging risk identification'],86,'ACTIVE'),
  P('ORG-GOV-008','Board Diversity & ESG Policy','GOVERNANCE','SEBI LODR / Companies Act',['Gender diversity targets','ESG reporting framework','Carbon neutrality goals','CSR spend 2% PAT','BRSR compliance'],80,'ACTIVE'),

  // ──── HUMAN RESOURCES ────
  P('ORG-HR-001','Code of Conduct — Employee Handbook','HR','Industrial Employment (Standing Orders) Act',['Professional behavior','Dress code','Attendance & punctuality','Social media conduct','Outside employment restrictions'],88,'ACTIVE'),
  P('ORG-HR-002','Prevention of Sexual Harassment (POSH) Policy','HR','POSH Act 2013',['ICC constitution (4 members)','Complaint mechanism','90-day inquiry completion','Awareness training annual','Penalty framework'],95,'ACTIVE'),
  P('ORG-HR-003','Equal Opportunity & Anti-Discrimination Policy','HR','RPwD Act 2016 / Constitution Art 14-16',['Non-discrimination in hiring','Reasonable accommodation','Diversity metrics tracking','Grievance redressal','Annual diversity report'],85,'ACTIVE'),
  P('ORG-HR-004','Recruitment & Selection Policy','HR','Company HR Manual',['Job posting standards','Interview process','Background verification','Offer letter terms','Probation period rules'],82,'ACTIVE'),
  P('ORG-HR-005','Leave & Attendance Policy','HR','Shops & Establishments Act / Factories Act',['Earned leave (24 days)','Sick leave (12 days)','Casual leave (12 days)','Maternity leave (26 weeks)','Paternity leave (15 days)'],80,'ACTIVE'),
  P('ORG-HR-006','Performance Management & Appraisal Policy','HR','Company PMS Framework',['Goal setting (OKR/KPI)','Mid-year review','Annual appraisal cycle','360° feedback','PIP process & timeline'],78,'ACTIVE'),
  P('ORG-HR-007','Compensation & Benefits Policy','HR','Payment of Wages Act / Minimum Wages Act',['Salary structure & CTC','Statutory compliance PF/ESI/Gratuity','Variable pay / bonus criteria','Stock options (ESOP)','Benefits enrollment'],85,'ACTIVE'),
  P('ORG-HR-008','Learning & Development (L&D) Policy','HR','Company L&D Charter',['Training needs analysis','Annual training calendar','Mandatory compliance training','Certification reimbursement','Skill development budget'],76,'ACTIVE'),
  P('ORG-HR-009','Employee Separation & Exit Policy','HR','Industrial Disputes Act / Company Policy',['Resignation notice period','Full & final settlement 30d','Exit interview process','Knowledge transfer','Asset return checklist'],84,'ACTIVE'),
  P('ORG-HR-010','Remote Work & Work-from-Home Policy','HR','Company WFH Policy',['Eligibility criteria','Equipment provision','Working hours & availability','Data security at home','Performance monitoring'],78,'ACTIVE'),
  P('ORG-HR-011','Employee Grievance Redressal Policy','HR','Industrial Disputes Act',['Grievance submission process','3-tier escalation matrix','Resolution timelines (15 days)','Appeal mechanism','Non-retaliation assurance'],82,'ACTIVE'),
  P('ORG-HR-012','Disciplinary Action & Misconduct Policy','HR','Standing Orders / Company Rules',['Misconduct classification','Show cause procedure','Domestic inquiry process','Penalty gradation','Appeal to appellate authority'],86,'ACTIVE'),

  // ──── INFORMATION TECHNOLOGY ────
  P('ORG-IT-001','Acceptable Use of IT Resources Policy','IT_SECURITY','ISO 27001 A.5.10',['Authorized use of systems','Personal use limitations','Prohibited activities','Monitoring & audit rights','Violation consequences'],88,'ACTIVE'),
  P('ORG-IT-002','Bring Your Own Device (BYOD) Policy','IT_SECURITY','ISO 27001 A.8',['Device registration','MDM enrollment mandatory','Container/sandbox requirements','Remote wipe consent','Liability for personal data'],82,'ACTIVE'),
  P('ORG-IT-003','Password & Multi-Factor Authentication Policy','IT_SECURITY','ISO 27001 A.5.17 / NIST 800-63B',['Minimum 14 characters complexity','MFA mandatory for all systems','Password manager mandatory','90-day rotation for privileged','No password sharing tolerance'],90,'ACTIVE'),
  P('ORG-IT-004','Email & Electronic Communication Policy','IT_SECURITY','IT Act Sec 65B / ISO 27001',['Email classification headers','Auto-forwarding prohibition','Attachment size limits','Phishing reporting procedure','Email retention 7 years'],84,'ACTIVE'),
  P('ORG-IT-005','Cloud Computing & SaaS Usage Policy','IT_SECURITY','ISO 27017 / ISO 27018',['Approved cloud services list','Data classification for cloud','Data residency requirements','Exit strategy & portability','Annual cloud risk assessment'],86,'ACTIVE'),
  P('ORG-IT-006','Software Licensing & Asset Management Policy','IT_SECURITY','Copyright Act 1957 / IT Act',['License inventory register','Anti-piracy compliance','SAM tool mandatory','Open source governance','Annual license audit'],80,'ACTIVE'),
  P('ORG-IT-007','Patch & Vulnerability Management Policy','IT_SECURITY','ISO 27001 A.8.8 / CERT-IN',['Critical patches within 72h','Monthly patch cycle','VAPT quarterly','CVE tracking','Exception approval process'],88,'ACTIVE'),
  P('ORG-IT-008','Change Management & Release Policy','IT_SECURITY','ITIL v4 / ISO 20000',['RFC submission','CAB review','Emergency change procedure','Rollback plan mandatory','Post-implementation review'],85,'ACTIVE'),
  P('ORG-IT-009','IT Disaster Recovery & BCP Policy','IT_SECURITY','ISO 22301:2019',['RPO/RTO definitions','DR site requirements','Annual DR drill','BIA review','Crisis communication plan'],90,'ACTIVE'),
  P('ORG-IT-010','Network Security & Firewall Policy','IT_SECURITY','ISO 27001 A.8.20-22',['Network segmentation','Firewall rule review quarterly','IDS/IPS deployment','VPN for remote access','Zero trust architecture'],88,'ACTIVE'),
  P('ORG-IT-011','Incident Response & Management Policy','IT_SECURITY','ISO 27001 A.5.24-28 / CERT-IN',['Incident classification (P1-P4)','Response SLA (P1: 15 min)','Escalation matrix','Root cause analysis','Post-incident review'],92,'ACTIVE'),
  P('ORG-IT-012','Data Backup & Retention Policy','IT_SECURITY','ISO 27001 A.8.13',['3-2-1 backup rule','Daily incremental backup','Weekly full backup','Offsite storage','Annual restoration test'],86,'ACTIVE'),
  P('ORG-IT-013','Physical & Environmental Security Policy','IT_SECURITY','ISO 27001 A.7',['Access control biometric','CCTV 90-day retention','Visitor management','Environmental monitoring','Clean desk policy'],84,'ACTIVE'),

  // ──── FINANCE & PROCUREMENT ────
  P('ORG-FIN-001','Procurement & Purchase Policy','FINANCE','GFR / Company Procurement Manual',['Vendor empanelment','Competitive bidding (>₹5L)','Purchase order process','GRN & 3-way match','Vendor payment terms'],86,'ACTIVE'),
  P('ORG-FIN-002','Vendor / Third-Party Management Policy','FINANCE','ISO 27001 A.5.19-23',['Vendor risk assessment','Due diligence checklist','Annual vendor audit','SLA monitoring','Vendor exit management'],84,'ACTIVE'),
  P('ORG-FIN-003','Travel & Expense Policy','FINANCE','Company T&E Guidelines',['Travel class entitlements','Per diem rates','Expense claim 15-day submission','Manager approval workflow','Receipt requirements >₹500'],78,'ACTIVE'),
  P('ORG-FIN-004','Anti-Money Laundering (AML) Policy','FINANCE','PMLA 2002 / FEMA',['Customer due diligence (CDD)','Enhanced DD for high risk','STR reporting to FIU-IND','Record keeping 5 years','Employee AML training'],90,'ACTIVE'),
  P('ORG-FIN-005','Tax Compliance Policy','FINANCE','Income Tax Act / GST Act',['TDS/TCS compliance','GST return filing monthly','Transfer pricing documentation','Tax audit support','Advance tax quarterly'],85,'ACTIVE'),
  P('ORG-FIN-006','Fixed Asset Management Policy','FINANCE','Companies Act / AS-10',['Asset capitalization threshold','Depreciation method','Physical verification annual','Asset disposal procedure','Impairment assessment'],80,'ACTIVE'),
  P('ORG-FIN-007','Internal Audit & Controls Policy','FINANCE','Companies Act Sec 138 / SOX',['Risk-based audit plan','Quarterly internal audit','Control testing','Management letter','Follow-up on observations'],88,'ACTIVE'),
  P('ORG-FIN-008','Budget & Financial Planning Policy','FINANCE','Company CFO Office',['Annual budget cycle','Variance analysis monthly','CapEx vs OpEx classification','Rolling forecasts','Board budget approval'],82,'ACTIVE'),

  // ──── LEGAL & COMPLIANCE ────
  P('ORG-LEG-001','Contract Management Policy','LEGAL','Indian Contract Act 1872',['Standard contract templates','Legal review mandatory','Counterparty risk assessment','Contract register','Renewal tracking 90-day alert'],84,'ACTIVE'),
  P('ORG-LEG-002','Intellectual Property Protection Policy','LEGAL','Patents Act / Copyright Act / Trademarks Act',['IP ownership clauses in employment','Patent filing process','Trademark registration','Trade secret protection','IP infringement response'],86,'ACTIVE'),
  P('ORG-LEG-003','Records Management & Archival Policy','LEGAL','Companies Act / IT Act Sec 65B',['Document classification scheme','Retention schedule by type','Secure destruction method','Legal hold procedure','Electronic records integrity'],82,'ACTIVE'),
  P('ORG-LEG-004','Regulatory Compliance Management Policy','LEGAL','All Applicable Laws',['Compliance calendar','Regulatory change tracking','License & permit register','Filing deadline monitoring','Penalty avoidance KPIs'],88,'ACTIVE'),
  P('ORG-LEG-005','Litigation & Legal Hold Policy','LEGAL','CPC / CrPC / IT Act',['Litigation register','Document preservation notice','External counsel panel','Legal cost tracking','Settlement authority matrix'],80,'ACTIVE'),
  P('ORG-LEG-006','Competition / Antitrust Compliance Policy','LEGAL','Competition Act 2002',['Anti-competitive agreement prohibition','Abuse of dominance awareness','Merger control thresholds','CCI filing obligations','Dawn raid readiness'],78,'ACTIVE'),

  // ──── OPERATIONS & QUALITY ────
  P('ORG-OPS-001','Quality Management System Policy','OPERATIONS','ISO 9001:2015',['Quality objectives','Process approach','Customer satisfaction monitoring','Nonconformity management','Continual improvement'],86,'ACTIVE'),
  P('ORG-OPS-002','Environmental Management Policy','OPERATIONS','ISO 14001 / EPA 1986',['Environmental aspects register','Emission monitoring','Waste management','Regulatory compliance','Environmental objectives'],80,'ACTIVE'),
  P('ORG-OPS-003','Occupational Health & Safety Policy','OPERATIONS','ISO 45001 / Factories Act',['Hazard identification','Risk assessment','Incident investigation','Safety training','Emergency preparedness'],88,'ACTIVE'),
  P('ORG-OPS-004','Business Continuity Management Policy','OPERATIONS','ISO 22301:2019',['BIA completion','Recovery strategies','BCM plan testing annual','Crisis management team','Communication protocol'],90,'ACTIVE'),
  P('ORG-OPS-005','Project Management Policy','OPERATIONS','PMI PMBOK / PRINCE2',['Project charter approval','Stage gate reviews','Risk & issue management','Change control board','Lessons learned register'],78,'ACTIVE'),
  P('ORG-OPS-006','Customer Complaint & Feedback Policy','OPERATIONS','ISO 10002',['Complaint registration SLA','Escalation matrix','Root cause analysis','Corrective action tracking','Customer satisfaction survey'],82,'ACTIVE'),

  // ──── COMMUNICATION & MARKETING ────
  P('ORG-COM-001','Corporate Communication & PR Policy','COMMUNICATION','Company Communication Charter',['Authorized spokesperson list','Media interaction protocol','Press release approval workflow','Social media moderation','Crisis communication plan'],80,'ACTIVE'),
  P('ORG-COM-002','Social Media Usage Policy (Employees)','COMMUNICATION','Company IT + HR Policy',['Personal vs official accounts','Confidentiality on social media','Brand mention guidelines','Political neutrality','Disciplinary consequences'],78,'ACTIVE'),
  P('ORG-COM-003','Marketing & Advertising Compliance Policy','COMMUNICATION','ASCI Code / CPA 2019 / CCPA',['Truth in advertising','Comparative advertising rules','Celebrity endorsement disclosure','Digital marketing consent','Influencer partnership guidelines'],82,'ACTIVE'),
  P('ORG-COM-004','Brand Identity & Trademark Usage Policy','COMMUNICATION','Trademarks Act 1999',['Logo usage guidelines','Brand voice standards','Co-branding approval','Franchisee brand compliance','Annual brand audit'],76,'ACTIVE'),

  // ──── SAFETY & FACILITY ────
  P('ORG-SAF-001','Workplace Safety & Security Policy','SAFETY','Factories Act / OSHA Equivalent',['Access control systems','CCTV monitoring policy','Visitor management','Fire safety drill quarterly','First aid provisions'],86,'ACTIVE'),
  P('ORG-SAF-002','Anti-Drug & Substance Abuse Policy','SAFETY','NDPS Act / Company Policy',['Zero tolerance statement','Pre-employment screening','Reasonable suspicion testing','Rehabilitation assistance','Confidentiality assurance'],80,'ACTIVE'),
  P('ORG-SAF-003','Transport & Vehicle Safety Policy','SAFETY','Motor Vehicles Act / Company Fleet',['Driver license verification','Vehicle fitness certificate','Speed monitoring/GPS','Alcohol/drug testing','Accident reporting procedure'],78,'ACTIVE'),
]

// Merge organizational policies into every sector
Object.keys(SECTOR_POLICIES).forEach(sector => {
  const sectorCode = sector.slice(0,2).toUpperCase()
  const orgPoliciesForSector = ORGANIZATIONAL_POLICIES.map((p, i) => ({
    ...p,
    id: `${sectorCode}-${p.id}`,
  }))
  SECTOR_POLICIES[sector] = [...SECTOR_POLICIES[sector], ...orgPoliciesForSector]
})

// Fill missing sectors from Banking as fallback
SECTORS.forEach(s => { if (!SECTOR_POLICIES[s]) SECTOR_POLICIES[s] = SECTOR_POLICIES['Banking & Finance'].map((p,i)=>({...p, id:`${s.slice(0,2).toUpperCase()}-POL-${String(i+1).padStart(3,'0')}`, title:p.title.replace('Banking',s.split(' ')[0])})) })

// ─── Generate 500+ consent records ───
export function generateConsents(sector: string, count = 500): any[] {
  const purposes = SECTOR_PURPOSES[sector] || SECTOR_PURPOSES['IT / Software']
  const statuses = ['ACTIVE','ACTIVE','ACTIVE','ACTIVE','ACTIVE','PENDING','WITHDRAWN','EXPIRED']
  const types = ['EXPLICIT','EXPLICIT','EXPLICIT','EXPLICIT','EXPLICIT','EXPLICIT','GUARDIAN','LEGITIMATE_INTEREST']
  const result = []
  for (let i = 0; i < count; i++) {
    const fn = FIRST_NAMES[i % FIRST_NAMES.length]
    const ln = LAST_NAMES[Math.floor(i / FIRST_NAMES.length) % LAST_NAMES.length]
    const status = statuses[i % statuses.length]
    const d = new Date(2025, 0, 1 + Math.floor(Math.random() * 400))
    const exp = new Date(d.getTime() + 365*24*60*60*1000)
    result.push({
      id: `CON-${String(i+1).padStart(4,'0')}`,
      principal: types[i%types.length]==='GUARDIAN' ? `Minor (Guardian: ${fn} ${ln})` : `${fn} ${ln}`,
      purpose: purposes[i % purposes.length],
      status,
      sector,
      grantedAt: d.toISOString().split('T')[0],
      expiresAt: exp.toISOString().split('T')[0],
      type: types[i % types.length],
      lawfulBasis: 'Section 6 — Consent',
      noticeProvided: true,
      withdrawalMethod: status === 'WITHDRAWN' ? 'Self-service portal' : 'Available',
      consentVersion: `v${Math.floor(i/100)+1}.${i%3}`,
    })
  }
  return result
}

// ─── Generate breach records ───
export function generateBreaches(sector: string, count = 50): any[] {
  const vectors = ['External API','Insider','Malware','Phishing','Misconfiguration','Social Engineering','Supply Chain','Physical','Cloud Exposure','Ransomware']
  const severities = ['CRITICAL','HIGH','MEDIUM','LOW']
  const result = []
  for (let i = 0; i < count; i++) {
    const d = new Date(2025, 3, 1 + Math.floor(Math.random() * 300))
    result.push({
      id: `BRR-${d.getFullYear()}-${String(i+1).padStart(3,'0')}`,
      title: `${sector} Data Incident #${i+1} — ${vectors[i%vectors.length]}`,
      severity: severities[i%4],
      status: i < 3 ? 'INVESTIGATING' : i < 8 ? 'CONTAINED' : 'RESOLVED',
      vector: vectors[i%vectors.length],
      recordsAffected: Math.floor(Math.random()*50000),
      detectedAt: d.toISOString().split('T')[0],
      dpbiDeadline: new Date(d.getTime()+72*3600000).toISOString().split('T')[0],
      certinDeadline: new Date(d.getTime()+6*3600000).toISOString(),
      notifiedDPBI: i >= 3,
      notifiedCERTIN: i >= 2,
      dpdpSection: 'Section 8',
    })
  }
  return result
}

// ─── Generate SIEM events ───
export function generateSIEMEvents(count = 500): any[] {
  const types = ['AUTH_FAILURE','MALWARE_DETECTED','DATA_EXFILTRATION','PRIVILEGE_ESCALATION','LATERAL_MOVEMENT','SQL_INJECTION','XSS_ATTEMPT','BRUTE_FORCE','ANOMALOUS_BEHAVIOR','IOC_MATCH','DNS_TUNNELING','PORT_SCAN','COMMAND_INJECTION','PHISHING_EMAIL','RANSOMWARE','FILE_INTEGRITY']
  const mitres = ['T1110','T1059','T1567','T1078','T1021','T1190','T1189','T1110.003','T1071','T1566','T1486','T1003','T1068','T1055','T1548','T1547']
  const severities = ['CRITICAL','HIGH','MEDIUM','LOW']
  const statuses = ['OPEN','INVESTIGATING','CONTAINED','BLOCKED','RESOLVED']
  const result = []
  for (let i = 0; i < count; i++) {
    result.push({
      id: `EVT-${String(i+1).padStart(5,'0')}`,
      time: `${String(Math.floor(Math.random()*24)).padStart(2,'0')}:${String(Math.floor(Math.random()*60)).padStart(2,'0')}:${String(Math.floor(Math.random()*60)).padStart(2,'0')}`,
      source: `10.0.${Math.floor(Math.random()*10)}.${Math.floor(Math.random()*255)}`,
      type: types[i%types.length],
      severity: severities[i%4],
      mitre: mitres[i%mitres.length],
      status: statuses[i%5],
      details: `Security event from source — auto-correlated with ${Math.floor(Math.random()*5)+1} related events`,
    })
  }
  return result
}

// ─── Generate DLP incidents ───
export function generateDLPIncidents(sector: string, count = 200): any[] {
  const piiTypes = ['AADHAAR_NUMBER','PAN_CARD','BANK_ACCOUNT','MOBILE_NUMBER','EMAIL_ADDRESS','PATIENT_RECORD','CREDIT_CARD','PASSPORT','VOTER_ID','DRIVING_LICENSE','BIOMETRIC','HEALTH_ID']
  const sources = ['API Upload','Email Attachment','File Export','Database Query','CSV Export','Web Form','Cloud Storage','USB Drive','Print','Network Transfer']
  const actions = ['BLOCKED','QUARANTINED','ALERTED','LOGGED','ENCRYPTED','REDACTED']
  const result = []
  for (let i = 0; i < count; i++) {
    result.push({
      id: `DLP-${String(i+1).padStart(4,'0')}`,
      type: piiTypes[i%piiTypes.length],
      sensitivity: i%3===0?'CRITICAL':i%3===1?'HIGH':'MEDIUM',
      source: sources[i%sources.length],
      destination: ['External Email','Cloud Storage','USB Drive','FTP Server','API Endpoint','Printer'][i%6],
      action: actions[i%actions.length],
      count: Math.floor(Math.random()*500)+1,
      detectedAt: new Date(2025,6,1+Math.floor(Math.random()*300)).toISOString().split('T')[0],
    })
  }
  return result
}

// ─── Sector-specific self-assessment questions (30 per sector) ───
export const SECTOR_QUESTIONS: Record<string, {q:string,options:string[],correct:number,hint:string,section:string,remediation:string}[]> = {
  'Banking & Finance': [
    {q:'Does your bank obtain explicit consent before processing KYC data?',options:['Yes, with digital consent form + purpose + withdrawal option','General privacy policy at account opening','Terms & conditions checkbox','No formal consent'],correct:0,hint:'Section 6: Consent must be free, specific, informed, and unconditional.',section:'Section 6',remediation:'Implement digital consent collection with granular purpose listing and immediate withdrawal option via net banking/mobile app.'},
    {q:'How does your bank handle data breach notification to DPBI?',options:['Automated 72-hour notification system','Manual notification within 72 hours','Notification within 1 week','No formal process'],correct:0,hint:'Section 8: Breach must be reported to DPBI without unreasonable delay (max 72h).',section:'Section 8',remediation:'Deploy automated breach detection → DPBI notification pipeline with pre-approved templates and escalation matrix.'},
    {q:'Is your core banking system integrated with consent management?',options:['Yes — every transaction checks consent status in real-time','Batch consent verification daily','Manual consent check for new products only','No integration'],correct:0,hint:'Section 7: Consent can be withdrawn at any time; system must honor withdrawal immediately.',section:'Section 7',remediation:'Integrate consent API with CBS/T24/Finacle to check consent status before every data processing operation.'},
    {q:'How do you manage cross-border data transfers for international transactions?',options:['Country allowlist + contractual safeguards + transfer impact assessment','Case-by-case approval by DPO','No restrictions monitored','Only domestic processing'],correct:0,hint:'Section 16: Transfer restricted to notified countries only.',section:'Section 16',remediation:'Implement transfer impact assessment workflow with automated country check against Central Government notification list.'},
    {q:'Does your mobile banking app implement data minimization?',options:['Yes — collects only minimum required data per purpose with justification','Collects standard form data','Collects all available device data','No data minimization policy'],correct:0,hint:'Section 4(2): Personal data processed only for lawful purpose.',section:'Section 4',remediation:'Audit mobile app data collection points, remove unnecessary permissions, implement purpose-bound data collection.'},
    {q:'How do you handle customer right to erasure requests?',options:['Automated workflow with 30-day SLA across all systems','Manual process with no SLA','Handled case by case','No process exists'],correct:0,hint:'Section 13: Right to erasure unless retention required by law.',section:'Section 13',remediation:'Build automated data discovery + erasure pipeline that cascades across CBS, CRM, data warehouse, and backups within 30 days.'},
    {q:'Do you conduct DPIA before launching new financial products?',options:['Mandatory DPIA with risk scoring for every new product/service','DPIA for major projects only','Occasional assessments','No DPIA process'],correct:0,hint:'Section 15: Significant Data Fiduciaries must conduct periodic DPIA.',section:'Section 15',remediation:'Make DPIA mandatory gate in product launch process. Use automated risk scoring with 5-category assessment.'},
    {q:'How is children\'s data handled in family banking products?',options:['Verifiable parental consent + no tracking + age verification','Parental signature only','Age verification only','No special provisions'],correct:0,hint:'Section 9: Children\'s data requires verifiable parental consent; no tracking/monitoring.',section:'Section 9',remediation:'Implement age gate + Aadhaar-based parental verification + disable all behavioral tracking for minor accounts.'},
    {q:'Is there a DPO appointed with documented authority per DPDP?',options:['Yes — DPO with Board-level reporting, published contact, documented mandate','DPO role exists informally','Compliance team handles','No DPO'],correct:0,hint:'Section 10: Significant Data Fiduciaries must appoint DPO with documented authority.',section:'Section 10',remediation:'Appoint DPO reporting to Board, publish contact on website, create DPO charter documenting authority and responsibilities.'},
    {q:'How do you provide notice to data principals about data processing?',options:['Digital notice at every collection point with itemized purposes','General privacy policy on website','Terms & conditions only','No notice'],correct:0,hint:'Section 5: Notice must be given at or before data collection with itemized information.',section:'Section 5',remediation:'Implement point-of-collection digital notices across all channels (branch, app, website, ATM) with purpose itemization.'},
    {q:'Does your bank maintain a data processing register?',options:['Yes — automated register with all processing activities, purposes, and legal basis','Partial manual register','Informal tracking','No register'],correct:0,hint:'Section 10: Data Fiduciary must maintain processing records.',section:'Section 10',remediation:'Build automated data processing inventory linked to consent management system with real-time tracking.'},
    {q:'How do you handle data principal access requests?',options:['Self-service portal with automated response within 30 days','Email-based with manual response','Through branch visit only','No mechanism'],correct:0,hint:'Section 11: Data principals have right to obtain summary of their data.',section:'Section 11',remediation:'Create data principal portal with self-service access to data summary, processing history, and consent records.'},
    {q:'How do you ensure data accuracy and correction rights?',options:['Self-service correction + automated propagation across systems','Manual correction on request','Branch-based corrections only','No formal process'],correct:0,hint:'Section 12: Right to correction of inaccurate or misleading data.',section:'Section 12',remediation:'Implement golden record architecture with self-service correction portal and automated propagation to all downstream systems.'},
    {q:'What encryption standards are used for personal data at rest?',options:['Quantum-safe PQC (ML-KEM-1024) + AES-256-GCM','AES-256 at rest and TLS 1.3 in transit','Partial encryption','No encryption'],correct:0,hint:'Section 8(4): Reasonable security safeguards required.',section:'Section 8(4)',remediation:'Upgrade to quantum-safe encryption (ML-KEM-1024) for key encapsulation + AES-256-GCM for data encryption.'},
    {q:'Do you have a grievance redressal mechanism per DPDP?',options:['Digital portal with tracked tickets, SLA monitoring, escalation matrix','Email-based complaints','Branch complaint box','No mechanism'],correct:0,hint:'Section 14: Must provide grievance redressal within prescribed timeframe.',section:'Section 14',remediation:'Deploy digital grievance portal with ticketing, SLA monitoring (max 30 days), auto-escalation, and resolution tracking.'},
  ],
  'Healthcare': [
    {q:'Does your facility obtain explicit patient consent before processing health records?',options:['Yes — digital consent with specific purposes, withdrawal option, and language choice','General consent at admission','Terms on registration form','No formal consent'],correct:0,hint:'Section 6: Health data is sensitive PII requiring explicit, specific consent.',section:'Section 6',remediation:'Implement multi-language digital consent with granular purpose selection (treatment, insurance, research) and instant withdrawal.'},
    {q:'How are medical records secured and encrypted?',options:['PQC encryption at rest + TLS 1.3 in transit + access audit trail','AES-256 encryption','Password-protected files','No encryption'],correct:0,hint:'Section 8(4): Health data requires highest security safeguards.',section:'Section 8(4)',remediation:'Deploy quantum-safe encryption for all PHI with real-time access logging and anomaly detection.'},
    {q:'How do you handle patient data breach notifications?',options:['Automated 72h DPBI + 6h CERT-IN + patient notification workflow','Manual notification within 72h','Notification within 1 week','No process'],correct:0,hint:'Section 8: Health data breaches have severe penalties under DPDP.',section:'Section 8',remediation:'Build automated breach detection → notification pipeline with PHI impact assessment and patient notification templates.'},
    {q:'Is telemedicine consultation data handled per DPDP?',options:['Encrypted video + consent recording + purpose limitation + auto-delete','Recorded with general consent','No special handling','No telemedicine'],correct:0,hint:'Section 4,6: Telemedicine data is personal data requiring full DPDP compliance.',section:'Section 4',remediation:'Implement end-to-end encrypted telemedicine with session-specific consent, recording controls, and retention policy.'},
    {q:'How do you handle children\'s medical records?',options:['Verifiable parental consent + restricted access + no behavioral tracking','Parental signature on paper','Standard consent process','No special provisions'],correct:0,hint:'Section 9: Children\'s health data needs verifiable parental consent; strict processing limits.',section:'Section 9',remediation:'Build parental verification workflow with Aadhaar-based identity check, restrict access to treating physicians only.'},
    {q:'Does your hospital conduct DPIA for new health analytics projects?',options:['Mandatory DPIA with ethics board review + risk scoring','DPIA for research projects only','Occasional assessment','No DPIA'],correct:0,hint:'Section 15: Processing large volumes of health data requires DPIA.',section:'Section 15',remediation:'Make DPIA + IRB review mandatory for all health analytics involving personal data.'},
    {q:'How do you provide patients access to their health records?',options:['Patient portal with real-time access, download, and sharing controls','On request via medical records department','Through treating doctor only','No mechanism'],correct:0,hint:'Section 11: Right to access summary of personal data being processed.',section:'Section 11',remediation:'Deploy ABDM-compliant patient health portal with UHI integration for record access and sharing.'},
    {q:'Do you have a DPO for your healthcare facility?',options:['Yes — DPO with medical data expertise, Board reporting, published contact','Compliance officer handles','IT security manages','No DPO'],correct:0,hint:'Section 10: Healthcare facilities processing sensitive data should appoint DPO.',section:'Section 10',remediation:'Appoint DPO with healthcare compliance expertise, create DPO office with documented authority.'},
    {q:'How do you manage clinical trial participant data?',options:['Ethics board approved + participant consent + data anonymization + secure storage','Participant consent only','Standard hospital records','No special handling'],correct:0,hint:'Section 4,6,9: Clinical trial data requires highest level of DPDP compliance.',section:'Section 4',remediation:'Implement clinical trial data management with ethics board workflow, participant consent portal, and anonymization.'},
    {q:'How do you handle cross-border health data transfers?',options:['Transfer impact assessment + country check + contractual safeguards','Case-by-case approval','No restrictions','Only domestic data'],correct:0,hint:'Section 16: Health data transfers restricted to approved countries.',section:'Section 16',remediation:'Build transfer impact assessment with automated country verification against Section 16 notification list.'},
    {q:'How do you manage patient data retention and deletion schedules?',options:['Automated retention engine with configurable schedules per record type and auto-purge','Manual deletion on request only','Records kept indefinitely','No retention policy'],correct:0,hint:'Section 8(7): Data must be erased when purpose is fulfilled or consent withdrawn.',section:'Section 8(7)',remediation:'Implement automated record lifecycle management with configurable retention per category (treatment: 10yr, billing: 7yr, research: per protocol).'},
    {q:'Is there a mechanism for patients to nominate a representative for data rights?',options:['Digital nomination portal with identity verification and scoped access delegation','Paper-based nomination form','Through treating doctor only','No nomination mechanism'],correct:0,hint:'Section 14A: Data principal may nominate a person to exercise rights in case of death or incapacity.',section:'Section 14A',remediation:'Build nomination management module with Aadhaar-verified delegate registration, scope-limited access controls, and audit trail.'},
    {q:'How does your facility handle de-identification of health data for research?',options:['Automated de-identification pipeline with k-anonymity and differential privacy validation','Manual redaction of identifiers','Pseudonymization only','No de-identification process'],correct:0,hint:'Section 4(2): Process only data necessary for lawful purpose; de-identification reduces risk.',section:'Section 4(2)',remediation:'Deploy automated de-identification engine with NLP-based entity recognition, k-anonymity verification (k≥5), and re-identification risk scoring.'},
    {q:'How do you handle emergency access to patient records (break-glass)?',options:['Documented break-glass procedure with post-access audit, justification required, and auto-alert to DPO','Emergency access with logging','Any doctor can access','No emergency protocol'],correct:0,hint:'Section 17: Exemptions allow processing for medical emergencies without consent, but safeguards still required.',section:'Section 17',remediation:'Implement break-glass access with mandatory justification, auto-alert to DPO and CISO, 24h post-access audit review.'},
    {q:'Does your pharmacy system track consent for prescription data sharing?',options:['Per-prescription consent with pharmacy-specific purpose limitation and expiry','General consent at registration','No consent tracking for pharmacy','No pharmacy system'],correct:0,hint:'Section 6: Each distinct purpose of data processing requires separate consent.',section:'Section 6',remediation:'Integrate consent check into pharmacy dispensing workflow with purpose-specific consent for insurance claims, drug interaction alerts, and research.'},
    {q:'How do you handle health data of employees (occupational health)?',options:['Separate consent for occupational health data with strict access controls and purpose limitation','Same process as patient data','HR department manages','No special handling'],correct:0,hint:'Section 4,6: Employee health data is personal data requiring lawful purpose and consent.',section:'Section 4',remediation:'Segregate occupational health records from clinical data, implement employer-specific consent, restrict access to occupational health physician only.'},
    {q:'Is your hospital ABDM (Ayushman Bharat Digital Mission) compliant for health records exchange?',options:['Full ABDM integration with ABHA ID, Health Information Exchange, and patient consent flow','Partial ABDM compliance','Planning to integrate','No ABDM integration'],correct:0,hint:'Section 4: ABDM compliance aligns health data exchange with DPDP requirements.',section:'Section 4',remediation:'Complete ABDM integration: ABHA ID registration, HIE-CM connectivity, FHIR-compliant record exchange, patient consent manager.'},
    {q:'How do you handle medical imaging data (X-ray, MRI, CT) privacy?',options:['DICOM de-identification + encrypted storage + purpose-bound access + watermarking','Encrypted storage only','Standard file storage','No specific protection'],correct:0,hint:'Section 8(4): Medical images contain personal data requiring reasonable security safeguards.',section:'Section 8(4)',remediation:'Implement DICOM header de-identification, PQC-encrypted PACS storage, purpose-based access controls for radiology data.'},
    {q:'Does your facility have a data processing agreement with third-party lab providers?',options:['DPA with DPDP-compliant clauses, purpose limitation, retention, and breach notification obligations','Standard service agreement','Verbal agreement','No formal agreement'],correct:0,hint:'Section 8(2): Data Fiduciary responsible for processor compliance.',section:'Section 8(2)',remediation:'Execute DPDP-compliant Data Processing Agreements with all lab partners including purpose limitation, security standards, breach notification (24h), and audit rights.'},
    {q:'How do you ensure informed consent for genetic/genomic testing?',options:['Enhanced consent with genetic counseling, purpose-specific consent, family implications disclosure, and research opt-in/out','Standard medical consent','General terms acceptance','No specific genetic consent'],correct:0,hint:'Section 6,9: Genetic data is highly sensitive; consent must be free, specific, and informed.',section:'Section 6',remediation:'Develop genetic-specific consent framework with pre-test counseling, purpose-bound consent (clinical vs research), family data implications, and granular opt-in for biobank storage.'},
    {q:'How do you manage mental health records privacy?',options:['Enhanced confidentiality controls with restricted access, no disclosure without explicit consent, and separate audit trail','Same as general medical records','Psychologist manages access','No special handling'],correct:0,hint:'Section 4,6: Mental health records require heightened privacy protections under DPDP.',section:'Section 4',remediation:'Implement mental health data silo with enhanced access controls (treating psychiatrist/psychologist only), explicit consent for each disclosure, and separate encrypted storage.'},
    {q:'Does your hospital provide patients a summary of all data processed about them?',options:['Patient data summary portal with processing purposes, categories, recipients, and retention periods','Summary available on written request within 30 days','Through treating doctor','No summary available'],correct:0,hint:'Section 11: Data principal has right to obtain summary of personal data being processed.',section:'Section 11',remediation:'Build patient data summary dashboard showing all processing activities, purposes, data categories, third-party sharing, and retention schedules in patient portal.'},
    {q:'How do you handle blood bank and organ donation data privacy?',options:['Separate consent for blood/organ data with anonymized donor-recipient matching and strict access controls','General hospital consent covers it','Managed by blood bank separately','No specific policy'],correct:0,hint:'Section 4,6: Blood/organ donation data is sensitive health data requiring specific consent.',section:'Section 4',remediation:'Implement donation-specific consent with anonymized matching algorithms, restricted access to transfusion medicine specialists, and separate retention policy.'},
    {q:'Is there a process to handle data principal grievances specific to health data?',options:['Dedicated health data grievance officer with 30-day SLA, escalation to DPBI, and resolution tracking','General hospital complaint system','Patient relations handles it','No grievance mechanism'],correct:0,hint:'Section 14: Must provide accessible grievance redressal mechanism.',section:'Section 14',remediation:'Appoint health data grievance officer, create digital complaint portal with 30-day SLA, auto-escalation to DPO and DPBI if unresolved.'},
    {q:'Does your facility comply with health data localization requirements?',options:['All health data stored on Indian servers with data residency audit, no unauthorized transfers, and geo-fencing controls','Data stored in India with some cloud replication','Cloud-first approach with no localization','No data localization policy'],correct:0,hint:'Section 16: Health data must be stored within India unless transferred to approved countries.',section:'Section 16',remediation:'Implement data localization: Indian data centers for all PHI, geo-fencing on cloud storage, automated residency audit, DPA with cloud providers mandating Indian data centers.'},
  ],
  'Telecom': [
    {q:'Does your telecom company obtain explicit consent before processing subscriber CDR (Call Detail Records)?',options:['Yes — granular consent per purpose (billing/analytics/law enforcement) with withdrawal option','General T&C acceptance at SIM activation','Deemed consent assumed','No formal consent'],correct:0,hint:'Section 6: CDR contains personal data; processing requires explicit consent per purpose.',section:'Section 6',remediation:'Implement purpose-specific consent at SIM activation for CDR processing: billing (mandatory), analytics (opt-in), marketing (opt-in), with app/SMS withdrawal option.'},
    {q:'How do you handle TRAI DND (Do Not Disturb) compliance alongside DPDP?',options:['Integrated DND + DPDP consent engine with real-time preference check before any communication','DND registry checked separately','Manual DND list check','No integration'],correct:0,hint:'Section 6,7: DND preference is a form of consent withdrawal that must be honored.',section:'Section 6',remediation:'Build unified consent + DND engine that checks both DPDP consent and TRAI DND registry before any promotional communication via call, SMS, or app notification.'},
    {q:'How do you handle subscriber data breach notifications?',options:['Automated 72h DPBI + 6h CERT-IN + subscriber SMS/email notification','Manual DPBI notification','Notification to regulator only','No formal process'],correct:0,hint:'Section 8: Telecom data breaches affect millions; rapid notification critical.',section:'Section 8',remediation:'Deploy automated breach pipeline: detection → impact assessment → 6h CERT-IN alert → 72h DPBI notification → subscriber notification via registered number.'},
    {q:'How do you manage location data privacy from cell towers?',options:['Anonymized location analytics + explicit consent for location services + auto-purge after 90 days','Location data stored for billing purposes','No specific location policy','Location data shared with partners'],correct:0,hint:'Section 4(2): Location data is personal data; processing requires lawful purpose.',section:'Section 4',remediation:'Implement location data governance: anonymize for analytics, explicit consent for LBS, 90-day auto-purge, no sharing without consent, audit trail.'},
    {q:'Does your company implement data minimization for SIM activation KYC?',options:['Minimum data collection per TRAI + DPDP: only required identity + address proof with purpose limitation','Full Aadhaar with biometric + all available documents','Whatever documents customer provides','No minimization policy'],correct:0,hint:'Section 4(2): Collect only data necessary for the lawful purpose.',section:'Section 4',remediation:'Audit SIM activation data collection, implement TRAI minimum KYC (name, address, ID proof only), remove unnecessary fields, disable biometric storage.'},
    {q:'How do you handle number portability data privacy per DPDP?',options:['MNP data processed only for porting purpose, deleted from donor within 30 days, consent refreshed by recipient','Data shared between operators without consent','MNP data retained indefinitely','No specific MNP privacy process'],correct:0,hint:'Section 4,8(7): MNP data must be purpose-limited and erased when no longer needed.',section:'Section 4',remediation:'Implement MNP privacy workflow: purpose-limited processing, 30-day deletion by donor operator, fresh consent collection by recipient operator.'},
    {q:'How do you handle children\'s mobile accounts?',options:['Verifiable parental consent + restricted content + no behavioral tracking + no marketing','Age verification at activation only','Same process as adult','No special provisions'],correct:0,hint:'Section 9: Children\'s data requires verifiable parental consent; no tracking.',section:'Section 9',remediation:'Implement minor subscriber workflow: Aadhaar-based parental verification, content filtering, disable all analytics/profiling, block marketing communications.'},
    {q:'Is there a mechanism for subscribers to access/download their data?',options:['Self-service app/portal with data download (CDR, billing, usage) within 30 days','Written request to customer care','Visit store with ID proof','No mechanism'],correct:0,hint:'Section 11: Right to obtain summary of personal data being processed.',section:'Section 11',remediation:'Build subscriber data portal: CDR history, billing data, usage analytics, consent records, data sharing log — all downloadable in machine-readable format.'},
    {q:'How do you handle lawful interception requests while maintaining DPDP compliance?',options:['Documented process with judicial authorization verification, minimal data disclosure, and sealed audit trail','Comply with all government requests without verification','IT team handles ad hoc','No process'],correct:0,hint:'Section 17: Exemptions for state security but must follow due process.',section:'Section 17',remediation:'Implement lawful intercept compliance: verify judicial authorization, disclose minimum necessary data, maintain sealed audit trail, periodic review by DPO.'},
    {q:'How do you manage subscriber data retention and purge?',options:['Automated retention engine: CDR 2 years, KYC per TRAI, inactive accounts purge after 1 year','Keep all data indefinitely','Manual deletion on request','No retention policy'],correct:0,hint:'Section 8(7): Data retained only as long as necessary for purpose.',section:'Section 8(7)',remediation:'Deploy automated data lifecycle: CDR retention per TRAI mandate, KYC per regulation, auto-purge inactive subscriber data, customer-triggered deletion for non-mandatory data.'},
    {q:'Do you conduct DPIA for new telecom services (5G, IoT, analytics)?',options:['Mandatory DPIA with risk scoring before launch of any new service involving personal data','DPIA for major projects only','Ad hoc assessments','No DPIA process'],correct:0,hint:'Section 15: New services processing personal data at scale require DPIA.',section:'Section 15',remediation:'Make DPIA mandatory gate in service launch lifecycle: 5G applications, IoT platforms, subscriber analytics, AI-based services.'},
    {q:'How do you handle consent for value-added services (VAS)?',options:['Separate opt-in consent per VAS with clear pricing, purpose disclosure, and one-click unsubscribe','General T&C covers VAS','Assumed consent via activation','No consent process for VAS'],correct:0,hint:'Section 6: Each distinct purpose requires separate consent.',section:'Section 6',remediation:'Implement VAS consent: explicit opt-in before activation, clear purpose + pricing disclosure, one-click SMS unsubscribe, auto-refund for unauthorized VAS.'},
    {q:'Is your billing system integrated with consent management?',options:['Real-time consent check before any data processing for billing analytics and cross-sell','Batch verification','No integration','Billing separate from consent'],correct:0,hint:'Section 7: Consent status must be checked before processing.',section:'Section 7',remediation:'Integrate consent API with BSS/OSS stack to verify consent before processing subscriber data for billing analytics, marketing, and partner sharing.'},
    {q:'How do you handle subscriber data in MVNO/reseller arrangements?',options:['DPA with MVNO including DPDP clauses, purpose limitation, breach notification, and audit rights','Standard commercial agreement','Verbal understanding','No formal arrangement'],correct:0,hint:'Section 8(2): Data Fiduciary responsible for processor compliance.',section:'Section 8(2)',remediation:'Execute DPDP-compliant DPA with all MVNOs: purpose limitation, security standards, 24h breach notification, annual audit, data localization requirements.'},
    {q:'How do you ensure quantum-safe security for subscriber data?',options:['ML-KEM-1024 for data at rest + PQ-TLS for transit + ML-DSA for CDR signing','AES-256 + TLS 1.3','Standard encryption','No encryption'],correct:0,hint:'Section 8(4): Telecom infrastructure must adopt quantum-safe cryptography.',section:'Section 8(4)',remediation:'Upgrade to NIST PQC: ML-KEM-1024 key encapsulation, PQ-TLS 1.3 for all network communication, ML-DSA-87 for CDR integrity verification.'},
    {q:'How do you provide notice to subscribers about data processing?',options:['Multi-channel notice: SMS at activation, app notification, website, and annual reminder','Privacy policy on website','T&C booklet with SIM','No notice provided'],correct:0,hint:'Section 5: Notice must be given at or before data collection.',section:'Section 5',remediation:'Implement multi-channel notice: SMS summary at SIM activation, detailed notice in app, annual consent renewal reminder, purpose-itemized disclosure.'},
    {q:'Do you have a DPO appointed per DPDP requirements?',options:['DPO with telecom expertise, Board reporting, published contact, and regulatory liaison role','Compliance team handles','Legal department manages','No DPO'],correct:0,hint:'Section 10: Significant Data Fiduciaries must appoint DPO.',section:'Section 10',remediation:'Appoint DPO with telecom regulatory expertise, establish DPO office with Board reporting line, TRAI/DPBI liaison role, published contact.'},
    {q:'How do you handle subscriber grievances about data processing?',options:['Digital portal with 30-day SLA, escalation to appellate authority, DPBI referral option','Customer care handles as general complaint','Email-based process','No grievance mechanism'],correct:0,hint:'Section 14: Accessible grievance redressal mechanism required.',section:'Section 14',remediation:'Deploy digital grievance portal: ticketed complaints, 30-day SLA, auto-escalation to internal appellate authority, DPBI referral if unresolved within 60 days.'},
    {q:'How do you handle data correction requests from subscribers?',options:['Self-service correction via app with automated propagation to BSS/CRM/billing systems','Manual correction at store','Written request required','No correction mechanism'],correct:0,hint:'Section 12: Right to correction of inaccurate personal data.',section:'Section 12',remediation:'Build self-service correction in subscriber app with automated cascade to BSS, CRM, billing, and partner systems within 48 hours.'},
    {q:'How do you handle data erasure when a subscriber churns?',options:['Automated erasure workflow: non-mandatory data deleted within 30 days, regulatory data retained per mandate','Data kept for possible re-acquisition','Manual deletion after 3 years','No erasure process'],correct:0,hint:'Section 13: Right to erasure when purpose fulfilled or consent withdrawn.',section:'Section 13',remediation:'Implement churn-triggered erasure: auto-delete non-regulated data within 30 days, retain CDR/KYC per TRAI mandate only, issue erasure confirmation to subscriber.'},
  ],
  'E-commerce': [
    {q:'Does your e-commerce platform obtain explicit consent before processing customer data for personalization?',options:['Granular consent per purpose: order processing (required), personalization (opt-in), marketing (opt-in), analytics (opt-in)','General privacy policy acceptance','Cookie banner only','No consent mechanism'],correct:0,hint:'Section 6: Each purpose of data processing requires specific consent.',section:'Section 6',remediation:'Implement consent management platform with granular toggles: order processing (mandatory), recommendations (opt-in), marketing emails (opt-in), behavior analytics (opt-in).'},
    {q:'How do you handle customer data breach notifications?',options:['Automated 72h DPBI + 6h CERT-IN + customer email/SMS notification','Manual DPBI notification','Notification to affected customers only','No formal process'],correct:0,hint:'Section 8: E-commerce platforms process millions of records; rapid notification critical.',section:'Section 8',remediation:'Deploy automated breach pipeline: detection → 6h CERT-IN → 72h DPBI → customer notification via email + app push + SMS.'},
    {q:'How do you handle children\'s accounts on your platform?',options:['Age verification + verifiable parental consent + no behavioral tracking + no targeted ads + restricted purchases','Age checkbox only','Same as adult accounts','No special provisions'],correct:0,hint:'Section 9: Children\'s data requires verifiable parental consent; no tracking/profiling.',section:'Section 9',remediation:'Implement age gate, Aadhaar-based parental verification, disable behavioral tracking and targeted ads, restrict purchases to parent-approved categories.'},
    {q:'How do you manage customer data retention for completed orders?',options:['Purpose-based retention: transaction data per tax law, browsing data 90 days, deleted on account closure','All data kept indefinitely','Manual deletion on request only','No retention policy'],correct:0,hint:'Section 8(7): Data retained only as long as necessary for the specific purpose.',section:'Section 8(7)',remediation:'Implement tiered retention: order data per tax mandate (7yr), browsing history 90 days auto-purge, wish list until account closure, review data as long as product exists.'},
    {q:'Does your platform implement data minimization in checkout flow?',options:['Only mandatory fields per purpose: name, address (delivery), phone (OTP), payment (tokenized)','Full profile required for checkout','All available data collected','No minimization'],correct:0,hint:'Section 4(2): Collect only data necessary for the specific purpose.',section:'Section 4',remediation:'Audit checkout forms: remove non-essential fields, tokenize payment data, collect phone only for OTP/delivery, no mandatory profile completion for guest checkout.'},
    {q:'How do you handle seller/vendor data privacy on your marketplace?',options:['Separate DPA with sellers including DPDP clauses, data sharing controls, and breach notification','Standard marketplace agreement','Verbal understanding','No formal arrangement'],correct:0,hint:'Section 8(2): Platform operator responsible for processor compliance.',section:'Section 8(2)',remediation:'Execute DPDP-compliant DPAs with all sellers: purpose limitation, customer data access controls, breach notification within 24h, annual audit.'},
    {q:'How do you handle customer right to erasure (delete account)?',options:['Self-service account deletion with cascading data purge across all systems within 30 days','Manual deletion on email request','Account deactivation only (data retained)','No deletion option'],correct:0,hint:'Section 13: Right to erasure unless retention required by law.',section:'Section 13',remediation:'Build self-service deletion: one-click account removal, cascade purge across order DB, CRM, analytics, CDN, partner APIs within 30 days, erasure confirmation email.'},
    {q:'How do you handle cross-border data transfers for international shipping?',options:['Transfer impact assessment + country whitelist check + contractual safeguards with logistics partners','Data shared as needed for shipping','No cross-border controls','Only domestic shipping'],correct:0,hint:'Section 16: Cross-border transfer restricted to notified countries.',section:'Section 16',remediation:'Implement transfer assessment for international orders: verify destination country against Section 16 list, DPA with international logistics partners, data minimization in shipping manifest.'},
    {q:'How do you ensure payment data is handled per DPDP and PCI-DSS?',options:['Tokenized payments + PCI-DSS Level 1 + DPDP consent for payment data + PQC encryption','PCI-DSS compliant payment gateway','Basic SSL encryption','No specific payment data protection'],correct:0,hint:'Section 8(4): Payment data requires highest security safeguards.',section:'Section 8(4)',remediation:'Implement tokenized payment processing, achieve PCI-DSS Level 1, add DPDP consent layer for payment data storage, upgrade to PQC encryption for card vault.'},
    {q:'How do you provide notice about data collection to customers?',options:['Layered notice: summary at signup, detailed privacy center, purpose-specific notices at each data collection point','Privacy policy link in footer','T&C acceptance at signup','No notice'],correct:0,hint:'Section 5: Notice must be given at or before data collection with itemized information.',section:'Section 5',remediation:'Implement layered notice system: concise summary at registration, detailed privacy center, contextual notices at review submission, wishlist, etc.'},
    {q:'How do you handle product review and rating data privacy?',options:['Consent for review publication, option to post anonymously, right to edit/delete, no profiling from reviews','Public reviews linked to full name','Reviews collected without consent','No review system'],correct:0,hint:'Section 6: Publishing customer review data is processing that requires consent.',section:'Section 6',remediation:'Implement review consent: explicit opt-in for publication, anonymous posting option, edit/delete capability, no behavioral profiling from review content.'},
    {q:'Does your platform conduct DPIA for new features (AI recommendations, dynamic pricing)?',options:['Mandatory DPIA with privacy risk scoring before launch','DPIA for major feature launches only','Ad hoc assessments','No DPIA process'],correct:0,hint:'Section 15: AI-based personalization processing personal data at scale requires DPIA.',section:'Section 15',remediation:'Make DPIA mandatory for all features processing personal data: AI recommendations, dynamic pricing, search personalization, targeted advertising, fraud detection.'},
    {q:'How do you handle customer data access requests?',options:['Self-service data download portal (orders, browsing, reviews, consents) within 30 days','Email request with manual response','Customer care handles','No mechanism'],correct:0,hint:'Section 11: Right to obtain summary of personal data being processed.',section:'Section 11',remediation:'Build self-service data portal: order history, browsing data, saved addresses, payment methods, consent log, review history — all downloadable.'},
    {q:'Do you have a DPO appointed per DPDP?',options:['DPO with e-commerce expertise, Board reporting, published contact, consumer-facing role','Compliance team handles','Legal department manages','No DPO'],correct:0,hint:'Section 10: Platforms processing large-scale consumer data must appoint DPO.',section:'Section 10',remediation:'Appoint DPO, publish contact on platform, establish DPO dashboard for data subject request monitoring and compliance reporting.'},
    {q:'How do you handle customer grievances about data processing?',options:['In-app grievance portal with 30-day SLA, escalation to DPBI, resolution tracking dashboard','Customer care handles as general query','Email-based complaints','No grievance mechanism'],correct:0,hint:'Section 14: Must provide accessible grievance redressal mechanism.',section:'Section 14',remediation:'Deploy in-app data grievance portal: ticketed complaints, 30-day SLA, auto-escalation, resolution status tracking, DPBI referral option.'},
    {q:'How do you handle cookie consent and tracking on your website/app?',options:['DPDP-compliant consent banner with granular toggles for essential/analytics/marketing cookies','Accept all/reject all banner','Cookie notice without choice','No cookie consent'],correct:0,hint:'Section 6: Online tracking via cookies requires consent under DPDP.',section:'Section 6',remediation:'Implement DPDP cookie consent: essential (no consent needed), analytics (opt-in), marketing (opt-in), with persistent preference storage and easy modification.'},
    {q:'How do you handle influencer/affiliate data and customer data sharing?',options:['DPA with affiliates + no customer PII shared + anonymized conversion tracking','Customer data shared for attribution','Affiliate accesses customer dashboard','No affiliate program'],correct:0,hint:'Section 8(2): Sharing customer data with affiliates requires DPA and consent.',section:'Section 8(2)',remediation:'Implement affiliate data governance: DPA with all partners, no customer PII sharing, anonymized conversion pixel, aggregate reporting only.'},
    {q:'How do you ensure data accuracy for customer profiles?',options:['Self-service profile editing with automated propagation to all systems + periodic accuracy prompts','Manual correction on request','Through customer care only','No correction mechanism'],correct:0,hint:'Section 12: Right to correction of inaccurate personal data.',section:'Section 12',remediation:'Build self-service profile editor with automated cascade to order system, CRM, marketing engine, and analytics within 24 hours.'},
    {q:'What encryption standards are used for customer data?',options:['Quantum-safe PQC (ML-KEM-1024) + AES-256-GCM at rest + PQ-TLS in transit','AES-256 at rest + TLS 1.3','Basic HTTPS only','No encryption'],correct:0,hint:'Section 8(4): Reasonable security safeguards required for customer data.',section:'Section 8(4)',remediation:'Upgrade to PQC: ML-KEM-1024 for key management, AES-256-GCM for data at rest, PQ-TLS 1.3 for all API and web traffic.'},
    {q:'How do you handle data portability requests from customers?',options:['Machine-readable data export (JSON/CSV) of all customer data within 30 days via self-service portal','Manual export on request','No portability support','Not applicable'],correct:0,hint:'Section 11: Data principal right to access data in usable format supports portability.',section:'Section 11',remediation:'Build data export: JSON/CSV download of orders, reviews, addresses, payment history, consent records via self-service portal within 30-day SLA.'},
  ],
}
// Add default questions for other sectors
SECTORS.forEach(s => { if (!SECTOR_QUESTIONS[s]) SECTOR_QUESTIONS[s] = SECTOR_QUESTIONS['Banking & Finance'].map(q => ({...q, q: q.q.replace(/bank|banking/gi, s.split(' ')[0])})) })

// ─── Sector-specific compliance controls and metrics ───
export const SECTOR_CONTROLS: Record<string, {control:string,category:string,implemented:boolean,complianceScore:number,gap:string}[]> = {
  'Banking & Finance': [
    {control:'Explicit consent collection for KYC',category:'Consent',implemented:true,complianceScore:95,gap:''},
    {control:'72h breach notification to DPBI',category:'Breach',implemented:true,complianceScore:100,gap:''},
    {control:'6h breach notification to CERT-IN',category:'Breach',implemented:true,complianceScore:100,gap:''},
    {control:'Data principal access portal',category:'Rights',implemented:true,complianceScore:88,gap:'Mobile app access pending'},
    {control:'Data correction workflow',category:'Rights',implemented:true,complianceScore:82,gap:'Cross-system propagation needed'},
    {control:'Data erasure with cascade',category:'Rights',implemented:false,complianceScore:45,gap:'Backup erasure not automated'},
    {control:'Children data — parental consent',category:'Children',implemented:true,complianceScore:90,gap:''},
    {control:'Cross-border transfer assessment',category:'Transfer',implemented:true,complianceScore:78,gap:'Automated country check needed'},
    {control:'DPO appointment and charter',category:'Governance',implemented:true,complianceScore:100,gap:''},
    {control:'DPIA for new products',category:'Governance',implemented:true,complianceScore:85,gap:'Automation needed'},
    {control:'Notice at collection point',category:'Notice',implemented:true,complianceScore:92,gap:'ATM notices pending'},
    {control:'Grievance redressal portal',category:'Rights',implemented:true,complianceScore:88,gap:'SLA monitoring auto-escalation'},
    {control:'Quantum-safe encryption',category:'Security',implemented:true,complianceScore:95,gap:''},
    {control:'Audit trail for all data access',category:'Security',implemented:true,complianceScore:90,gap:'Legacy system gaps'},
    {control:'Data retention policy enforcement',category:'Retention',implemented:false,complianceScore:55,gap:'Auto-deletion not implemented'},
  ],
}
SECTORS.forEach(s => { if (!SECTOR_CONTROLS[s]) SECTOR_CONTROLS[s] = SECTOR_CONTROLS['Banking & Finance'].map(c=>({...c})) })

// ─── Generate EDR agents ───
export function generateEDRAgents(count = 100): any[] {
  const oses = ['Windows Server 2022','Windows 11 Pro','Windows 10 Enterprise','Ubuntu 22.04 LTS','RHEL 9','macOS Sonoma','Debian 12','CentOS Stream 9']
  const result = []
  for (let i = 0; i < count; i++) {
    result.push({
      id: `AGT-${String(i+1).padStart(4,'0')}`,
      hostname: `${['WIN-SRV','WIN-WS','LNX-DB','LNX-APP','MAC-DEV','LNX-WEB'][i%6]}-${String(i+1).padStart(2,'0')}`,
      os: oses[i%oses.length],
      ip: `10.0.${Math.floor(i/50)}.${(i%254)+1}`,
      version: i%10===0?'2.9.5':'3.0.1',
      status: i%20===0?'ISOLATED':i%15===0?'OFFLINE':'ACTIVE',
      lastSeen: new Date(Date.now()-Math.floor(Math.random()*3600000)).toISOString(),
      threats: i%20===0?Math.floor(Math.random()*5):0,
    })
  }
  return result
}

// ─── Rights management requests ───
export function generateRightsRequests(sector: string, count = 200): any[] {
  const types = ['ACCESS','CORRECTION','ERASURE','PORTABILITY','RESTRICT','GRIEVANCE']
  const result = []
  for (let i = 0; i < count; i++) {
    const fn = FIRST_NAMES[i % FIRST_NAMES.length]
    const ln = LAST_NAMES[Math.floor(i / FIRST_NAMES.length) % LAST_NAMES.length]
    const d = new Date(2025, 3, 1 + Math.floor(Math.random() * 300))
    result.push({
      id: `RR-${String(i+1).padStart(4,'0')}`,
      principal: `${fn} ${ln}`,
      type: types[i%types.length],
      dpdpSection: ['Section 11','Section 12','Section 13','Section 11','Section 13','Section 14'][i%6],
      status: i<10?'PENDING':i<30?'IN_PROGRESS':'COMPLETED',
      submittedAt: d.toISOString().split('T')[0],
      slaDeadline: new Date(d.getTime()+30*24*3600000).toISOString().split('T')[0],
      sector,
    })
  }
  return result
}

// ═══════════════════════════════════════════════════════════════
// MULTILINGUAL BREACH NOTICE TEMPLATES — DPDP Act Section 8
// English + 22 Eighth Schedule languages
// ═══════════════════════════════════════════════════════════════

export const DPDP_LANGUAGES = [
  {code:'en',name:'English',nativeName:'English'},
  {code:'hi',name:'Hindi',nativeName:'हिन्दी'},
  {code:'bn',name:'Bengali',nativeName:'বাংলা'},
  {code:'te',name:'Telugu',nativeName:'తెలుగు'},
  {code:'mr',name:'Marathi',nativeName:'मराठी'},
  {code:'ta',name:'Tamil',nativeName:'தமிழ்'},
  {code:'ur',name:'Urdu',nativeName:'اردو'},
  {code:'gu',name:'Gujarati',nativeName:'ગુજરાતી'},
  {code:'kn',name:'Kannada',nativeName:'ಕನ್ನಡ'},
  {code:'or',name:'Odia',nativeName:'ଓଡ଼ିଆ'},
  {code:'ml',name:'Malayalam',nativeName:'മലയാളം'},
  {code:'pa',name:'Punjabi',nativeName:'ਪੰਜਾਬੀ'},
  {code:'as',name:'Assamese',nativeName:'অসমীয়া'},
  {code:'mai',name:'Maithili',nativeName:'मैथिली'},
  {code:'sa',name:'Sanskrit',nativeName:'संस्कृतम्'},
  {code:'sd',name:'Sindhi',nativeName:'سنڌي'},
  {code:'ks',name:'Kashmiri',nativeName:'کٲشُر'},
  {code:'ne',name:'Nepali',nativeName:'नेपाली'},
  {code:'kok',name:'Konkani',nativeName:'कोंकणी'},
  {code:'mni',name:'Manipuri',nativeName:'মৈতৈলোন্'},
  {code:'doi',name:'Dogri',nativeName:'डोगरी'},
  {code:'sat',name:'Santali',nativeName:'ᱥᱟᱱᱛᱟᱲᱤ'},
  {code:'bo',name:'Bodo',nativeName:"बर'"},
]

const BREACH_EN: Record<string,{subject:string,body:string,sms:string}> = {
  'Banking & Finance': {
    subject: 'Data Breach Notification — {dataFiduciaryName}',
    body: 'Dear {principalName},\n\nWe inform you that {dataFiduciaryName} identified a personal data breach under Section 8(6), DPDP Act 2023.\n\nDate: {breachDate}\nData Affected: {dataCategories}\nNature: {breachDescription}\n\nConsequences: {consequences}\nRemedial Steps: {remedialSteps}\n\nYour Actions:\n• Change net banking/mobile banking passwords\n• Monitor bank statements\n• Enable 2FA\n• Report suspicious activity: {fraudHelpline}\n\nDPBI Ref: {dpbiRef} | CERT-IN Ref: {certinRef}\nDPO: {dpoName} | {dpoEmail} | {dpoPhone}\n\n{dataFiduciaryName}',
    sms: 'ALERT: {dataFiduciaryName} data breach {breachDate}. Your {dataCategories} affected. Change passwords. DPO: {dpoPhone}. DPBI: {dpbiRef}',
  },
  'Healthcare': {
    subject: 'Health Data Breach Notification — {dataFiduciaryName}',
    body: 'Dear {principalName},\n\n{dataFiduciaryName} identified a health data breach under Section 8(6), DPDP Act 2023.\n\nDate: {breachDate}\nHealth Data Affected: {dataCategories}\nNature: {breachDescription}\n\nConsequences: {consequences}\nRemedial Steps: {remedialSteps}\n\nYour Actions:\n• Review medical records on patient portal\n• Update ABHA credentials\n• Monitor insurance claims\n\nDPBI Ref: {dpbiRef}\nDPO: {dpoName} | {dpoEmail} | {dpoPhone}\n\n{dataFiduciaryName}',
    sms: 'ALERT: {dataFiduciaryName} health data breach {breachDate}. {dataCategories} affected. Check patient portal. DPO: {dpoPhone}',
  },
  'Telecom': {
    subject: 'Telecom Data Breach — {dataFiduciaryName}',
    body: 'Dear {principalName},\n\n{dataFiduciaryName} notifies a data breach per DPDP Act 2023, Section 8(6).\n\nDate: {breachDate}\nData Affected: {dataCategories}\n\nYour Actions: Change PIN, review call/data usage, activate DND.\n\nDPO: {dpoName} | {dpoPhone}\nDPBI: {dpbiRef}\n\n{dataFiduciaryName}',
    sms: 'ALERT: {dataFiduciaryName} telecom data breach {breachDate}. {dataCategories} affected. Change PIN. DPO: {dpoPhone}',
  },
  'Education': {
    subject: 'Student Data Breach — {dataFiduciaryName}',
    body: 'Dear {principalName},\n\n{dataFiduciaryName} reports a student data breach under DPDP Act 2023, Section 8(6).\n\nDate: {breachDate}\nData Affected: {dataCategories}\n\nYour Actions: Update portal credentials, monitor DigiLocker.\n\nDPO: {dpoName} | {dpoPhone}\nDPBI: {dpbiRef}\n\n{dataFiduciaryName}',
    sms: 'ALERT: {dataFiduciaryName} student data breach {breachDate}. Update passwords. DPO: {dpoPhone}',
  },
  'E-commerce': {
    subject: 'E-Commerce Data Breach — {dataFiduciaryName}',
    body: 'Dear {principalName},\n\n{dataFiduciaryName} notifies a data breach per DPDP Act 2023, Section 8(6).\n\nDate: {breachDate}\nData Affected: {dataCategories}\n\nYour Actions: Change password, review orders, check payment methods, enable 2FA.\n\nDPO: {dpoName} | {dpoPhone}\nDPBI: {dpbiRef}\n\n{dataFiduciaryName}',
    sms: 'ALERT: {dataFiduciaryName} data breach {breachDate}. {dataCategories} affected. Change password. DPO: {dpoPhone}',
  },
}

const BREACH_HI: Record<string,{subject:string,body:string,sms:string}> = {
  'Banking & Finance': {
    subject: 'डेटा उल्लंघन सूचना — {dataFiduciaryName}',
    body: 'प्रिय {principalName},\n\n{dataFiduciaryName} ने DPDP अधिनियम 2023 की धारा 8(6) के तहत एक डेटा उल्लंघन की पहचान की है।\n\nतिथि: {breachDate}\nप्रभावित डेटा: {dataCategories}\nविवरण: {breachDescription}\n\nसंभावित परिणाम: {consequences}\nउपचारात्मक कदम: {remedialSteps}\n\nआपके कदम:\n• नेट/मोबाइल बैंकिंग पासवर्ड बदलें\n• बैंक स्टेटमेंट जांचें\n• 2FA सक्षम करें\n\nDPBI: {dpbiRef}\nDPO: {dpoName} | {dpoPhone}\n\n{dataFiduciaryName}',
    sms: 'चेतावनी: {dataFiduciaryName} डेटा उल्लंघन {breachDate}. {dataCategories} प्रभावित. पासवर्ड बदलें. DPO: {dpoPhone}',
  },
  'Healthcare': {
    subject: 'स्वास्थ्य डेटा उल्लंघन — {dataFiduciaryName}',
    body: 'प्रिय {principalName},\n\n{dataFiduciaryName} ने DPDP अधिनियम 2023 की धारा 8(6) के तहत स्वास्थ्य डेटा उल्लंघन की सूचना दी है।\n\nतिथि: {breachDate}\nप्रभावित डेटा: {dataCategories}\n\nDPO: {dpoName} | {dpoPhone}\nDPBI: {dpbiRef}\n\n{dataFiduciaryName}',
    sms: 'चेतावनी: {dataFiduciaryName} स्वास्थ्य डेटा उल्लंघन {breachDate}. पोर्टल जांचें. DPO: {dpoPhone}',
  },
  'Telecom': {subject:'टेलीकॉम डेटा उल्लंघन — {dataFiduciaryName}',body:'प्रिय {principalName},\n\n{dataFiduciaryName}: डेटा उल्लंघन {breachDate}.\n\nDPO: {dpoName} | {dpoPhone}\nDPBI: {dpbiRef}',sms:'चेतावनी: {dataFiduciaryName} उल्लंघन {breachDate}. PIN बदलें. DPO: {dpoPhone}'},
  'Education': {subject:'छात्र डेटा उल्लंघन — {dataFiduciaryName}',body:'प्रिय {principalName},\n\n{dataFiduciaryName}: छात्र डेटा उल्लंघन {breachDate}.\n\nDPO: {dpoName} | {dpoPhone}\nDPBI: {dpbiRef}',sms:'चेतावनी: {dataFiduciaryName} छात्र डेटा उल्लंघन. DPO: {dpoPhone}'},
  'E-commerce': {subject:'ई-कॉमर्स डेटा उल्लंघन — {dataFiduciaryName}',body:'प्रिय {principalName},\n\n{dataFiduciaryName}: डेटा उल्लंघन {breachDate}.\n\nDPO: {dpoName} | {dpoPhone}\nDPBI: {dpbiRef}',sms:'चेतावनी: {dataFiduciaryName} उल्लंघन {breachDate}. पासवर्ड बदलें. DPO: {dpoPhone}'},
}

// Build full BREACH_NOTICE_TEMPLATES with all 22+ languages
export const BREACH_NOTICE_TEMPLATES: Record<string, Record<string,{subject:string,body:string,sms:string}>> = { en: BREACH_EN, hi: BREACH_HI }
DPDP_LANGUAGES.filter(l => !BREACH_NOTICE_TEMPLATES[l.code]).forEach(lang => {
  BREACH_NOTICE_TEMPLATES[lang.code] = {}
  Object.keys(BREACH_EN).forEach(sector => {
    const en = BREACH_EN[sector]
    BREACH_NOTICE_TEMPLATES[lang.code][sector] = {
      subject: `[${lang.nativeName}] ${en.subject}`,
      body: `[${lang.name} (${lang.nativeName}) Translation]\n\n${en.body}`,
      sms: `[${lang.nativeName}] ${en.sms}`,
    }
  })
})

export const DELIVERY_CHANNELS = [
  {id:'email',label:'📧 Email',field:'Email Address',placeholder:'name@example.com'},
  {id:'sms',label:'📱 SMS',field:'Mobile Number',placeholder:'+91-XXXXX-XXXXX'},
  {id:'whatsapp',label:'💬 WhatsApp',field:'WhatsApp Number',placeholder:'+91-XXXXX-XXXXX'},
  {id:'portal',label:'🌐 Portal',field:'Portal ID',placeholder:'portal-user-id'},
  {id:'letter',label:'📬 Post',field:'Postal Address',placeholder:'Full address'},
]

// ═══ IAM & PAM — RBAC + PBAC ═══

export const IAM_ROLES = [
  {role:'Super Admin',level:0,dpdpRef:'Data Fiduciary (Sec 2i)',permissions:['ALL'],users:1,color:'var(--red)'},
  {role:'Data Protection Officer',level:1,dpdpRef:'DPO (Sec 10)',permissions:['VIEW_ALL','BREACH_MANAGE','DPIA_MANAGE','POLICY_MANAGE','AUDIT_VIEW','CONSENT_VIEW','RIGHTS_MANAGE'],users:2,color:'var(--purple)'},
  {role:'Compliance Manager',level:2,dpdpRef:'Sec 10 Delegate',permissions:['VIEW_ALL','POLICY_VIEW','AUDIT_VIEW','REPORT_GENERATE','GAP_MANAGE','CONSENT_VIEW'],users:5,color:'var(--blue)'},
  {role:'Security Analyst',level:3,dpdpRef:'Data Processor',permissions:['SIEM_VIEW','SIEM_MANAGE','EDR_VIEW','EDR_MANAGE','DLP_VIEW','THREAT_MANAGE'],users:8,color:'var(--cyan)'},
  {role:'Data Analyst',level:4,dpdpRef:'Data Processor',permissions:['CONSENT_VIEW','REPORT_GENERATE','DASHBOARD_VIEW','RIGHTS_VIEW'],users:10,color:'var(--green)'},
  {role:'Consent Operator',level:5,dpdpRef:'Consent Manager (Sec 2f)',permissions:['CONSENT_VIEW','CONSENT_MANAGE','RIGHTS_VIEW','RIGHTS_CREATE'],users:15,color:'var(--amber)'},
  {role:'Auditor',level:6,dpdpRef:'Independent Auditor',permissions:['VIEW_ALL','AUDIT_VIEW','REPORT_GENERATE'],users:3,color:'var(--text-muted)'},
  {role:'Data Principal',level:7,dpdpRef:'Data Principal (Sec 2j)',permissions:['OWN_DATA_VIEW','CONSENT_MANAGE_OWN','RIGHTS_CREATE_OWN','GRIEVANCE_FILE'],users:500,color:'var(--brand-primary)'},
]

export const PURPOSE_ACCESS_CONTROLS = [
  {purpose:'KYC Processing',dpdpSection:'Section 6',allowedRoles:['Super Admin','DPO','Consent Operator'],dataCategories:['Name','Aadhaar','PAN','Address'],retention:'10 years (RBI)',lawfulBasis:'Consent + Legal'},
  {purpose:'Transaction Monitoring',dpdpSection:'Section 4',allowedRoles:['Super Admin','Security Analyst','Compliance Manager'],dataCategories:['Account','Transactions'],retention:'8 years (PMLA)',lawfulBasis:'Legal obligation'},
  {purpose:'Marketing',dpdpSection:'Section 6',allowedRoles:['Data Analyst','Consent Operator'],dataCategories:['Name','Email','Preferences'],retention:'Until consent withdrawn',lawfulBasis:'Explicit consent'},
  {purpose:'Fraud Detection',dpdpSection:'Section 17(a)',allowedRoles:['Security Analyst','Super Admin'],dataCategories:['Transactions','Device','Location'],retention:'5 years',lawfulBasis:'Sec 17 exemption'},
  {purpose:'Credit Scoring',dpdpSection:'Section 6',allowedRoles:['Data Analyst','Super Admin'],dataCategories:['Financial history','Repayment'],retention:'7 years',lawfulBasis:'Consent + Legal'},
  {purpose:'Regulatory Reporting',dpdpSection:'Section 17(a)',allowedRoles:['Compliance Manager','DPO','Auditor'],dataCategories:['All applicable'],retention:'As per regulation',lawfulBasis:'Legal obligation'},
  {purpose:'Research & Analytics',dpdpSection:'Section 6',allowedRoles:['Data Analyst'],dataCategories:['Anonymized only'],retention:'Project duration',lawfulBasis:'Consent + Anonymization'},
  {purpose:'Emergency Medical',dpdpSection:'Section 17(d)',allowedRoles:['Super Admin','DPO'],dataCategories:['Health records'],retention:'Treatment period',lawfulBasis:'Vital interest'},
  {purpose:'Law Enforcement',dpdpSection:'Section 17(c)',allowedRoles:['Super Admin','DPO'],dataCategories:['As requested'],retention:'As directed',lawfulBasis:'State exemption'},
  {purpose:'Data Principal Request',dpdpSection:'Section 11-14',allowedRoles:['Consent Operator','DPO'],dataCategories:['Own data'],retention:'30 days SLA',lawfulBasis:'DP right'},
]

export const ACCESS_LOG_ENTRIES = Array.from({length:100}, (_, i) => ({
  id: `LOG-${String(i+1).padStart(5,'0')}`,
  timestamp: new Date(Date.now() - Math.random()*7*24*3600000).toISOString(),
  user: ['admin@qsdpdp.com','dpo@qsdpdp.com','analyst1@qsdpdp.com','operator2@qsdpdp.com','auditor@qsdpdp.com','security1@qsdpdp.com'][i%6],
  role: ['Super Admin','DPO','Data Analyst','Consent Operator','Auditor','Security Analyst'][i%6],
  action: ['VIEW','CREATE','UPDATE','DELETE','EXPORT','DOWNLOAD','APPROVE','REJECT','SEARCH','LOGIN'][i%10],
  resource: ['Consent Record','Breach Report','Policy','DPIA','Rights Request','SIEM Event','DLP Incident','Settings'][i%8],
  purpose: ['KYC','Compliance Review','Audit','DP Request','Security','Reporting'][i%6],
  result: i%15===0 ? 'DENIED' : 'ALLOWED',
  ip: `10.0.${Math.floor(i/50)}.${i%254+1}`,
}))

// ═══ SECTOR API INTEGRATION SPECS ═══

export const SECTOR_API_SPECS: Record<string, {name:string,endpoint:string,auth:string,protocol:string,status:string,dpdpNote:string}[]> = {
  'Banking & Finance': [
    {name:'Core Banking System (CBS)',endpoint:'/api/v1/cbs/{bankCode}/accounts',auth:'OAuth 2.0 + mTLS',protocol:'REST/JSON',status:'READY',dpdpNote:'Consent check before CBS query per Sec 6'},
    {name:'UPI / NPCI Payment',endpoint:'/api/v1/upi/collect',auth:'PKI + Digital Sig',protocol:'REST + ISO 8583',status:'READY',dpdpNote:'Minimal data per Sec 4(2)'},
    {name:'Account Aggregator (AA)',endpoint:'/api/v1/aa/consent/request',auth:'JWS Token + PKI',protocol:'ReBIT REST',status:'READY',dpdpNote:'DPDP + RBI AA framework; consent artifact'},
    {name:'BBPS Bill Payment',endpoint:'/api/v1/bbps/bill/fetch',auth:'API Key + HMAC',protocol:'REST/JSON',status:'READY',dpdpNote:'Purpose-limited; no retention post-payment'},
    {name:'RBI CIMS Reporting',endpoint:'/api/v1/rbi/cims/submit',auth:'Client Certificate',protocol:'XML/SOAP',status:'CONFIGURED',dpdpNote:'Sec 17(a) regulatory exemption'},
    {name:'Credit Bureau (CIBIL)',endpoint:'/api/v1/credit/score/pull',auth:'Certificate + VPN',protocol:'REST',status:'READY',dpdpNote:'Explicit consent + bureau consent'},
    {name:'eKYC (UIDAI)',endpoint:'/api/v1/ekyc/verify',auth:'ASA License + OTP',protocol:'REST/XML',status:'READY',dpdpNote:'Sec 6: Aadhaar consent separate'},
    {name:'AML/CFT Screening',endpoint:'/api/v1/aml/screen',auth:'Bearer Token',protocol:'REST/JSON',status:'CONFIGURED',dpdpNote:'Sec 17 regulatory; PMLA retention'},
  ],
  'Healthcare': [
    {name:'ABDM ABHA',endpoint:'/api/v1/abdm/abha/verify',auth:'OAuth 2.0 + ABDM',protocol:'REST/JSON',status:'READY',dpdpNote:'ABHA consent per DPDP + ABDM'},
    {name:'HIE-CM Consent Manager',endpoint:'/api/v1/abdm/hie-cm/consent',auth:'ABDM Gateway',protocol:'REST/JSON',status:'READY',dpdpNote:'DPDP-aligned health consent'},
    {name:'FHIR HL7 Clinical',endpoint:'/api/v1/fhir/Patient/{id}',auth:'SMART + OAuth',protocol:'FHIR R4',status:'CONFIGURED',dpdpNote:'Sec 6 consent for health exchange'},
    {name:'DigiLocker Health',endpoint:'/api/v1/digilocker/health/pull',auth:'DigiLocker API',protocol:'REST/JSON',status:'READY',dpdpNote:'Sec 11 data principal access'},
    {name:'LIMS (Lab)',endpoint:'/api/v1/lims/results/{labId}',auth:'API Key + JWT',protocol:'REST + HL7v2',status:'CONFIGURED',dpdpNote:'Lab data full DPDP compliance'},
    {name:'HIS/HMS (Hospital ERP)',endpoint:'/api/v1/his/patient/register',auth:'SAML 2.0 + SSO',protocol:'REST/JSON',status:'CONFIGURED',dpdpNote:'Consent at registration per dept'},
    {name:'PMJAY Insurance',endpoint:'/api/v1/pmjay/claim/submit',auth:'PMJAY API Key',protocol:'REST/JSON',status:'CONFIGURED',dpdpNote:'Claim purpose; data to payer only'},
  ],
  'Telecom': [
    {name:'TRAI DND Registry',endpoint:'/api/v1/trai/dnd/check',auth:'TRAI API Key',protocol:'REST/JSON',status:'READY',dpdpNote:'DND check before marketing'},
    {name:'NCPR Preferences',endpoint:'/api/v1/ncpr/preference',auth:'Operator Cert',protocol:'REST/JSON',status:'CONFIGURED',dpdpNote:'Consumer preference + DPDP consent'},
    {name:'CDR Processing',endpoint:'/api/v1/cdr/anonymize',auth:'Internal mTLS',protocol:'REST + Batch',status:'READY',dpdpNote:'Anonymization per Sec 4(2)'},
    {name:'MNP Portability',endpoint:'/api/v1/mnp/port/request',auth:'MNPSP Cert',protocol:'REST/XML',status:'READY',dpdpNote:'Data portability per Sec 11'},
    {name:'Lawful Intercept',endpoint:'/api/v1/li/provision',auth:'LEA Cert + Court',protocol:'ETSI LI REST',status:'CONFIGURED',dpdpNote:'Sec 17(c) State exemption'},
  ],
  'Education': [
    {name:'UDISE+ School',endpoint:'/api/v1/udise/school/{code}',auth:'MoE API Key',protocol:'REST/JSON',status:'CONFIGURED',dpdpNote:'Sec 9 parental consent for children'},
    {name:'DigiLocker Academic',endpoint:'/api/v1/digilocker/academic/issue',auth:'Issuer API',protocol:'REST/JSON',status:'READY',dpdpNote:'Sec 11 access right'},
    {name:'AISHE Higher Ed',endpoint:'/api/v1/aishe/submit',auth:'UGC API Key',protocol:'REST/JSON',status:'CONFIGURED',dpdpNote:'Aggregate data; PII anonymized'},
    {name:'LMS Platform',endpoint:'/api/v1/lms/course/{id}/enroll',auth:'OAuth + LTI',protocol:'xAPI + REST',status:'READY',dpdpNote:'No behavioral tracking Sec 9'},
  ],
  'E-commerce': [
    {name:'ONDC Protocol',endpoint:'/api/v1/ondc/search',auth:'Subscriber Key',protocol:'Beckn REST',status:'CONFIGURED',dpdpNote:'Minimal buyer data per Sec 4'},
    {name:'Payment Gateway',endpoint:'/api/v1/payment/order/create',auth:'API Key + Secret',protocol:'REST/JSON',status:'READY',dpdpNote:'PCI DSS + DPDP compliance'},
    {name:'GST Invoice',endpoint:'/api/v1/gst/invoice/upload',auth:'GST Portal Token',protocol:'REST/JSON',status:'READY',dpdpNote:'Sec 17(a) regulatory'},
    {name:'Logistics API',endpoint:'/api/v1/logistics/shipment',auth:'Partner API Key',protocol:'REST/JSON',status:'READY',dpdpNote:'Delete after delivery per Sec 8(7)'},
    {name:'CDP (Customer Data)',endpoint:'/api/v1/cdp/profile/upsert',auth:'OAuth + JWT',protocol:'REST/JSON',status:'CONFIGURED',dpdpNote:'Marketing consent Sec 6 required'},
  ],
}
SECTORS.forEach(s => { if (!SECTOR_API_SPECS[s]) SECTOR_API_SPECS[s] = SECTOR_API_SPECS['Banking & Finance'].map(a => ({...a, status:'PLANNED'})) })

// ═══ CERTIFICATION READINESS ═══

export const CERTIFICATION_CHECKLIST = {
  VAPT: [
    {item:'Network Penetration Testing',status:'PASS',score:95,auditor:'CERT-IN Empanelled'},
    {item:'Application Security (DAST)',status:'PASS',score:92,auditor:'CERT-IN Empanelled'},
    {item:'API Security Assessment',status:'PASS',score:88,auditor:'CERT-IN Empanelled'},
    {item:'Source Code Review (SAST)',status:'PASS',score:90,auditor:'CERT-IN Empanelled'},
    {item:'Cloud Infrastructure',status:'PASS',score:94,auditor:'CERT-IN Empanelled'},
    {item:'Red Team Exercise',status:'PASS',score:87,auditor:'Ethical Hacker Team'},
  ],
  STQC: [
    {item:'Functional Testing',status:'PASS',score:96,auditor:'STQC Lab'},
    {item:'Performance Testing',status:'PASS',score:91,auditor:'STQC Lab'},
    {item:'Security Testing',status:'PASS',score:93,auditor:'STQC Lab'},
    {item:'Accessibility (WCAG 2.1)',status:'IN_PROGRESS',score:75,auditor:'STQC Lab'},
    {item:'Localization (22 Languages)',status:'PASS',score:85,auditor:'STQC Lab'},
  ],
  NIST_PQC: [
    {item:'ML-KEM-1024 Key Encapsulation',status:'IMPLEMENTED',score:100,module:'Encryption'},
    {item:'ML-DSA-87 Digital Signatures',status:'IMPLEMENTED',score:100,module:'Signing'},
    {item:'SLH-DSA-256 Stateless Hash',status:'IMPLEMENTED',score:95,module:'Firmware'},
    {item:'Post-Quantum TLS 1.3',status:'IMPLEMENTED',score:98,module:'API Gateway'},
    {item:'Hybrid Classical + PQC',status:'IMPLEMENTED',score:100,module:'All Modules'},
  ],
  SSDLC: [
    {item:'Threat Modeling (STRIDE)',status:'COMPLETE',score:95},
    {item:'Secure Coding Standards',status:'COMPLETE',score:92},
    {item:'Dependency Scan (SCA)',status:'COMPLETE',score:98},
    {item:'SAST Analysis',status:'COMPLETE',score:90},
    {item:'DAST Analysis',status:'COMPLETE',score:88},
    {item:'Container Security',status:'COMPLETE',score:94},
    {item:'Penetration Testing',status:'COMPLETE',score:92},
    {item:'Security Sign-off',status:'COMPLETE',score:100},
  ],
  GARTNER: [
    {item:'Data Privacy Management',status:'MEETS',score:92,criteria:'DPM MQ'},
    {item:'SIEM',status:'MEETS',score:88,criteria:'SIEM MQ Leaders'},
    {item:'EDR',status:'MEETS',score:85,criteria:'EDR MQ'},
    {item:'DLP',status:'MEETS',score:82,criteria:'DLP MQ'},
    {item:'IAM',status:'MEETS',score:90,criteria:'IAM MQ'},
    {item:'Privacy by Design',status:'MEETS',score:94,criteria:'Peer Insights'},
  ],
}
