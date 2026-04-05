; ═══════════════════════════════════════════════════════════════════
; QS-DPDP Enterprise — Premium MS Office-Style Installer
; Features: Install / Repair / Uninstall maintenance mode
; Theme: Blue gradient top, white bottom, orange accent border
; ═══════════════════════════════════════════════════════════════════

#define MyAppName      "QS-DPDP Enterprise"
#define MyAppVersion   "1.0.0"
#define MyAppPublisher "QualityShield Technologies Pvt. Ltd."
#define MyAppURL       "https://qsdpdp.com"
#define MyAppExeName   "ndcp.exe"

[Setup]
AppId={{B7A2C8D4-1F6E-4A90-BCDE-567890ABCDEF}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} v{#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
DefaultDirName={autopf}\QualityShield\QS-DPDP Enterprise
DefaultGroupName=QualityShield\{#MyAppName}
AllowNoIcons=yes
OutputDir=d:\N_DPDP\QS-DPDP-Enterprise\target\final-installer
OutputBaseFilename=QS-DPDP-Enterprise-Setup
SetupIconFile=d:\N_DPDP\cmd\ndcp\build\windows\icon.ico
UninstallDisplayIcon={app}\icon.ico
WizardImageFile=d:\N_DPDP\QS-DPDP-Enterprise\installer-assets\wizard.bmp
WizardSmallImageFile=d:\N_DPDP\QS-DPDP-Enterprise\installer-assets\wizard-small.bmp
WizardStyle=modern
DisableWelcomePage=no
DisableDirPage=no
DisableProgramGroupPage=yes
DisableReadyPage=no
DisableFinishedPage=no
Compression=lzma2/ultra64
SolidCompression=yes
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
VersionInfoVersion=1.0.0.0
VersionInfoCompany={#MyAppPublisher}
VersionInfoDescription={#MyAppName} - DPDP Act 2023 Compliance Platform
VersionInfoProductName={#MyAppName}
VersionInfoProductVersion={#MyAppVersion}
CreateUninstallRegKey=yes
UninstallDisplayName={#MyAppName}
; Allow reinstall over existing
UsePreviousAppDir=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
WelcomeLabel1=Welcome to {#MyAppName} Setup
WelcomeLabel2=This wizard will install {#MyAppName} v{#MyAppVersion} on your computer.%n%n━━━ Enterprise Compliance Operating System ━━━%n%n✦ Compliance Dashboard & Real-time Scoring%n✦ Consent Lifecycle Management%n✦ Breach Detection & 72-hour SLA Notifications%n✦ DPIA Impact Assessments%n✦ PII Scanner & Data Classification%n✦ Q-SIEM, Q-SOAR & Q-DLP Modules%n✦ Audit Trail & Gap Analysis%n✦ Assessment Reports (CERT-IN grade)%n✦ Policy Engine & Controls Framework%n%nClick Next to continue, or Cancel to exit.
FinishedHeadingLabel=Setup Complete — Ready to Launch!
FinishedLabel={#MyAppName} v{#MyAppVersion} has been installed.%n%n━━━ Login Credentials ━━━%n%n  DPO (Full Access):  dpo / dpo123%n  Admin:  admin / admin123%n  Auditor:  auditor / audit123%n%nClick Finish to launch the application.%n%n© 2024 QualityShield Technologies Pvt. Ltd.
ReadyLabel1=Ready to Install
ReadyLabel2a=Setup is ready to install {#MyAppName} on your computer.%n%nInstallation includes:%n  • QS-DPDP Enterprise Application (12.7 MB)%n  • Desktop & Start Menu shortcuts%n%nClick Install to proceed.

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop shortcut"; Flags: checkedonce

[Files]
; Native Desktop Application
Source: "d:\N_DPDP\cmd\ndcp\ndcp.exe"; DestDir: "{app}"; DestName: "{#MyAppExeName}"; Flags: ignoreversion
; Application icon
Source: "d:\N_DPDP\cmd\ndcp\build\windows\icon.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
; Desktop shortcut
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; IconFilename: "{app}\icon.ico"; Tasks: desktopicon
; Start Menu entries
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; IconFilename: "{app}\icon.ico"
Name: "{group}\Uninstall {#MyAppName}"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; Description: "Launch {#MyAppName}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}\data"
Type: filesandordirs; Name: "{app}\logs"
Type: filesandordirs; Name: "{app}"

[Code]
// ═══════════════════════════════════════════════
// MAINTENANCE MODE: Install / Repair / Uninstall
// ═══════════════════════════════════════════════
var
  MaintenancePage: TWizardPage;
  RepairRadio: TNewRadioButton;
  UninstallRadio: TNewRadioButton;
  IsRepairMode: Boolean;
  IsUninstallMode: Boolean;
  IsAlreadyInstalled: Boolean;

function GetDefaultDir(): String;
begin
  Result := ExpandConstant('{autopf}') + '\QualityShield\QS-DPDP Enterprise';
end;

function IsAppInstalled(): Boolean;
begin
  Result := FileExists(GetDefaultDir() + '\{#MyAppExeName}');
end;

procedure MaintenancePageActivate(Sender: TWizardPage);
begin
  // Style the page
  WizardForm.NextButton.Caption := 'Continue';
end;

function MaintenancePageNextClick(Sender: TWizardPage): Boolean;
var
  UninsPath: String;
  ResultCode: Integer;
begin
  Result := True;

  if UninstallRadio.Checked then
  begin
    IsUninstallMode := True;
    // Run uninstaller
    UninsPath := GetDefaultDir() + '\unins000.exe';
    if FileExists(UninsPath) then
    begin
      Exec(UninsPath, '/SILENT', '', SW_SHOW, ewWaitUntilTerminated, ResultCode);
    end;
    // Cancel the current install wizard after uninstall
    WizardForm.Close;
    Result := False;
  end
  else if RepairRadio.Checked then
  begin
    IsRepairMode := True;
    // Continue with install (which will overwrite = repair)
    Result := True;
  end;
end;

procedure InitializeWizard();
var
  TitleLabel: TLabel;
  DescLabel: TLabel;
  InstallRadio: TNewRadioButton;
  DividerPanel: TPanel;
begin
  // Check if already installed
  IsAlreadyInstalled := IsAppInstalled();
  IsRepairMode := False;
  IsUninstallMode := False;

  if IsAlreadyInstalled then
  begin
    // Create maintenance page (after Welcome, before Dir selection)
    MaintenancePage := CreateCustomPage(wpWelcome,
      'Modify Installation',
      'QS-DPDP Enterprise is already installed. Choose an option:');

    // Title
    TitleLabel := TLabel.Create(MaintenancePage);
    TitleLabel.Parent := MaintenancePage.Surface;
    TitleLabel.Caption := 'What would you like to do?';
    TitleLabel.Font.Size := 11;
    TitleLabel.Font.Style := [fsBold];
    TitleLabel.Left := 0;
    TitleLabel.Top := 10;

    // Divider
    DividerPanel := TPanel.Create(MaintenancePage);
    DividerPanel.Parent := MaintenancePage.Surface;
    DividerPanel.Left := 0;
    DividerPanel.Top := 38;
    DividerPanel.Width := MaintenancePage.SurfaceWidth;
    DividerPanel.Height := 2;
    DividerPanel.BevelOuter := bvNone;
    DividerPanel.Color := $00D77800;  // Orange accent

    // Repair option
    RepairRadio := TNewRadioButton.Create(MaintenancePage);
    RepairRadio.Parent := MaintenancePage.Surface;
    RepairRadio.Caption := 'Repair — Reinstall application files';
    RepairRadio.Font.Size := 10;
    RepairRadio.Left := 10;
    RepairRadio.Top := 55;
    RepairRadio.Width := MaintenancePage.SurfaceWidth - 20;
    RepairRadio.Checked := True;

    DescLabel := TLabel.Create(MaintenancePage);
    DescLabel.Parent := MaintenancePage.Surface;
    DescLabel.Caption := 'Repairs the installation by reinstalling all application files.';
    DescLabel.Font.Color := clGray;
    DescLabel.Left := 30;
    DescLabel.Top := 78;

    // Uninstall option
    UninstallRadio := TNewRadioButton.Create(MaintenancePage);
    UninstallRadio.Parent := MaintenancePage.Surface;
    UninstallRadio.Caption := 'Uninstall — Remove QS-DPDP Enterprise';
    UninstallRadio.Font.Size := 10;
    UninstallRadio.Left := 10;
    UninstallRadio.Top := 110;
    UninstallRadio.Width := MaintenancePage.SurfaceWidth - 20;

    DescLabel := TLabel.Create(MaintenancePage);
    DescLabel.Parent := MaintenancePage.Surface;
    DescLabel.Caption := 'Completely removes the application and all its components.';
    DescLabel.Font.Color := clGray;
    DescLabel.Left := 30;
    DescLabel.Top := 133;

    // Feature reminder
    DividerPanel := TPanel.Create(MaintenancePage);
    DividerPanel.Parent := MaintenancePage.Surface;
    DividerPanel.Left := 0;
    DividerPanel.Top := 170;
    DividerPanel.Width := MaintenancePage.SurfaceWidth;
    DividerPanel.Height := 2;
    DividerPanel.BevelOuter := bvNone;
    DividerPanel.Color := $00D77800;

    DescLabel := TLabel.Create(MaintenancePage);
    DescLabel.Parent := MaintenancePage.Surface;
    DescLabel.Caption :=
      'Installed: QS-DPDP Enterprise v1.0.0' + #13#10 +
      'Publisher: QualityShield Technologies Pvt. Ltd.' + #13#10 +
      'Location: ' + GetDefaultDir();
    DescLabel.Font.Color := $00996600;
    DescLabel.Left := 10;
    DescLabel.Top := 180;
    DescLabel.Width := MaintenancePage.SurfaceWidth - 20;
    DescLabel.Height := 60;

    MaintenancePage.OnActivate := @MaintenancePageActivate;
    MaintenancePage.OnNextButtonClick := @MaintenancePageNextClick;
  end;
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  // If already installed, skip directory page (use existing dir for repair)
  if IsAlreadyInstalled and (PageID = wpSelectDir) and RepairRadio.Checked then
    Result := True;
end;

// ═══════════════════════════════════════════════
// POST-INSTALL: Show desktop with shortcut
// ═══════════════════════════════════════════════
procedure ShowDesktopAfterInstall();
var
  ResultCode: Integer;
begin
  Exec('powershell.exe',
    '-NoProfile -ExecutionPolicy Bypass -Command "Add-Type -AssemblyName System.Windows.Forms; (New-Object -ComObject Shell.Application).MinimizeAll(); Start-Sleep -Milliseconds 800; [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point(80, 100)"',
    '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssDone then
    ShowDesktopAfterInstall();
end;
