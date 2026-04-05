Add-Type -AssemblyName System.Drawing

function Create-GoogleQIcon {
    param([int]$Size, [string]$OutPath)

    $bmp = New-Object System.Drawing.Bitmap $Size, $Size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'HighQuality'
    $g.InterpolationMode = 'HighQualityBicubic'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $g.PixelOffsetMode = 'HighQuality'

    # Google brand colors
    $blue = [System.Drawing.Color]::FromArgb(66, 133, 244)    # #4285F4
    $red = [System.Drawing.Color]::FromArgb(234, 67, 53)     # #EA4335
    $yellow = [System.Drawing.Color]::FromArgb(251, 188, 5)     # #FBBC05
    $green = [System.Drawing.Color]::FromArgb(52, 168, 83)     # #34A853

    # White background rounded rectangle (squircle)
    $g.Clear([System.Drawing.Color]::Transparent)
    $margin = [int]($Size * 0.02)
    $radius = [int]($Size * 0.22)
    $bgPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $w = $Size - 2 * $margin
    $h = $Size - 2 * $margin
    $bgPath.AddArc($margin, $margin, $radius, $radius, 180, 90)
    $bgPath.AddArc($margin + $w - $radius, $margin, $radius, $radius, 270, 90)
    $bgPath.AddArc($margin + $w - $radius, $margin + $h - $radius, $radius, $radius, 0, 90)
    $bgPath.AddArc($margin, $margin + $h - $radius, $radius, $radius, 90, 90)
    $bgPath.CloseFigure()

    # Subtle shadow
    $shadowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(30, 0, 0, 0))
    $shadowPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $sOff = [int]($Size * 0.015)
    $shadowPath.AddArc($margin + $sOff, $margin + $sOff, $radius, $radius, 180, 90)
    $shadowPath.AddArc($margin + $w - $radius + $sOff, $margin + $sOff, $radius, $radius, 270, 90)
    $shadowPath.AddArc($margin + $w - $radius + $sOff, $margin + $h - $radius + $sOff, $radius, $radius, 0, 90)
    $shadowPath.AddArc($margin + $sOff, $margin + $h - $radius + $sOff, $radius, $radius, 90, 90)
    $shadowPath.CloseFigure()
    $g.FillPath($shadowBrush, $shadowPath)

    # White background
    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $g.FillPath($whiteBrush, $bgPath)

    # ═══════ Draw the "Q" using Google colors ═══════
    # The Q is drawn as colored arc segments like Google's "G" icon style
    $cx = [int]($Size * 0.50)
    $cy = [int]($Size * 0.42)
    $outerR = [int]($Size * 0.28)
    $innerR = [int]($Size * 0.18)
    $penW = [int]($Size * 0.10)

    # Draw Q as 4 colored arc segments (like Google's G)
    # Blue: top-left (180° to 270°)
    $penBlue = New-Object System.Drawing.Pen($blue, $penW)
    $penBlue.StartCap = 'Round'
    $penBlue.EndCap = 'Flat'
    $arcRect = New-Object System.Drawing.Rectangle ($cx - $outerR), ($cy - $outerR), ($outerR * 2), ($outerR * 2)
    $g.DrawArc($penBlue, $arcRect, 180, 90)

    # Red: top-right (270° to 360°)
    $penRed = New-Object System.Drawing.Pen($red, $penW)
    $penRed.StartCap = 'Flat'
    $penRed.EndCap = 'Flat'
    $g.DrawArc($penRed, $arcRect, 270, 90)

    # Yellow: bottom-right (0° to 70°)
    $penYellow = New-Object System.Drawing.Pen($yellow, $penW)
    $penYellow.StartCap = 'Flat'
    $penYellow.EndCap = 'Flat'
    $g.DrawArc($penYellow, $arcRect, 0, 70)

    # Green: bottom-left (90° to 180°)
    $penGreen = New-Object System.Drawing.Pen($green, $penW)
    $penGreen.StartCap = 'Flat'
    $penGreen.EndCap = 'Round'
    $g.DrawArc($penGreen, $arcRect, 90, 90)

    # Q tail: diagonal line from center-right going down-right (blue color)
    $tailPen = New-Object System.Drawing.Pen($blue, [int]($penW * 0.7))
    $tailPen.StartCap = 'Flat'
    $tailPen.EndCap = 'Round'
    $tailX1 = [int]($cx + $outerR * 0.3)
    $tailY1 = [int]($cy + $outerR * 0.5)
    $tailX2 = [int]($cx + $outerR * 1.05)
    $tailY2 = [int]($cy + $outerR * 1.15)
    $g.DrawLine($tailPen, $tailX1, $tailY1, $tailX2, $tailY2)

    # Horizontal bar at center-right (like Google's G bar) in blue
    $barPen = New-Object System.Drawing.Pen($blue, [int]($penW * 0.65))
    $barPen.StartCap = 'Flat'
    $barPen.EndCap = 'Round'
    $barX1 = $cx
    $barY1 = $cy
    $barX2 = [int]($cx + $outerR)
    $barY2 = $cy
    $g.DrawLine($barPen, $barX1, $barY1, $barX2, $barY2)

    # ═══════ Draw "DPDP" text at bottom ═══════
    $dpdpSize = [int]($Size * 0.11)
    $fontDPDP = New-Object System.Drawing.Font('Segoe UI', $dpdpSize, [System.Drawing.FontStyle]::Bold)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = 'Center'
    $sf.LineAlignment = 'Center'

    # Draw each letter in a different Google color
    $dpdpText = "DPDP"
    $dpdpColors = @($blue, $red, $yellow, $green)
    $totalWidth = 0
    $charWidths = @()
    for ($i = 0; $i -lt $dpdpText.Length; $i++) {
        $charSize = $g.MeasureString($dpdpText[$i].ToString(), $fontDPDP)
        $charWidths += $charSize.Width * 0.82
        $totalWidth += $charSize.Width * 0.82
    }
    $startX = ($Size - $totalWidth) / 2
    $textY = [int]($Size * 0.77)

    for ($i = 0; $i -lt $dpdpText.Length; $i++) {
        $charBrush = New-Object System.Drawing.SolidBrush($dpdpColors[$i])
        $g.DrawString($dpdpText[$i].ToString(), $fontDPDP, $charBrush, $startX, $textY)
        $startX += $charWidths[$i]
        $charBrush.Dispose()
    }

    $g.Dispose()
    return $bmp
}

function Save-AsIco {
    param([System.Drawing.Bitmap]$Bitmap, [string]$Path)

    $stream = [System.IO.File]::Create($Path)
    $bw = New-Object System.IO.BinaryWriter($stream)

    # Create multiple sizes for ICO
    $sizes = @(256, 48, 32, 16)
    $pngDatas = @()

    foreach ($sz in $sizes) {
        $resized = New-Object System.Drawing.Bitmap $sz, $sz
        $gx = [System.Drawing.Graphics]::FromImage($resized)
        $gx.SmoothingMode = 'HighQuality'
        $gx.InterpolationMode = 'HighQualityBicubic'
        $gx.DrawImage($Bitmap, 0, 0, $sz, $sz)
        $gx.Dispose()

        $ms = New-Object System.IO.MemoryStream
        $resized.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
        $pngDatas += , ($ms.ToArray())
        $ms.Close()
        $resized.Dispose()
    }

    # ICO header
    $bw.Write([Int16]0)              # Reserved
    $bw.Write([Int16]1)              # Type = ICO
    $bw.Write([Int16]$sizes.Count)   # Image count

    # Calculate data offset (header = 6, each entry = 16)
    $dataOffset = 6 + ($sizes.Count * 16)

    # Write directory entries
    for ($i = 0; $i -lt $sizes.Count; $i++) {
        $sz = $sizes[$i]
        $bw.Write([byte]$(if ($sz -eq 256) { 0 } else { $sz }))  # Width (0 = 256)
        $bw.Write([byte]$(if ($sz -eq 256) { 0 } else { $sz }))  # Height
        $bw.Write([byte]0)            # Colors
        $bw.Write([byte]0)            # Reserved
        $bw.Write([Int16]1)           # Planes
        $bw.Write([Int16]32)          # BPP
        $bw.Write([Int32]$pngDatas[$i].Length)  # Size
        $bw.Write([Int32]$dataOffset)           # Offset
        $dataOffset += $pngDatas[$i].Length
    }

    # Write PNG data
    foreach ($data in $pngDatas) {
        $bw.Write($data)
    }

    $bw.Close()
    $stream.Close()
}

# ═══════ GENERATE ALL ASSETS ═══════
Write-Host "Creating Google-style Q icon..."

$icon256 = Create-GoogleQIcon -Size 256 -OutPath ""
$icoPath = "d:\N_DPDP\QS-DPDP-Enterprise\installer-assets\app.ico"
Save-AsIco -Bitmap $icon256 -Path $icoPath
Write-Host "Icon: $icoPath"

# Save as PNG too (for JavaFX splash)
$pngPath = "d:\N_DPDP\QS-DPDP-Enterprise\src\main\resources\images\app-icon.png"
New-Item -ItemType Directory -Path (Split-Path $pngPath) -Force | Out-Null
$icon256.Save($pngPath, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "PNG: $pngPath"

# ═══════ Wizard Side Panel (164x314) ═══════
Write-Host "Creating wizard side panel..."
$wiz = New-Object System.Drawing.Bitmap 164, 314
$g = [System.Drawing.Graphics]::FromImage($wiz)
$g.SmoothingMode = 'HighQuality'
$g.TextRenderingHint = 'AntiAliasGridFit'

# Dark gradient background
$wizRect = New-Object System.Drawing.Rectangle 0, 0, 164, 314
$gBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($wizRect, [System.Drawing.Color]::FromArgb(15, 23, 42), [System.Drawing.Color]::FromArgb(30, 41, 59), 180)
$g.FillRectangle($gBrush, $wizRect)

# Draw mini Q icon (scaled down)
$miniIcon = New-Object System.Drawing.Bitmap 80, 80
$gm = [System.Drawing.Graphics]::FromImage($miniIcon)
$gm.SmoothingMode = 'HighQuality'
$gm.InterpolationMode = 'HighQualityBicubic'
$gm.DrawImage($icon256, 0, 0, 80, 80)
$gm.Dispose()
$g.DrawImage($miniIcon, 42, 40, 80, 80)

# Product name
$sf = New-Object System.Drawing.StringFormat
$sf.Alignment = 'Center'
$fontBrand = New-Object System.Drawing.Font('Segoe UI', 13, [System.Drawing.FontStyle]::Bold)
$g.DrawString('QS-DPDP', $fontBrand, [System.Drawing.Brushes]::White, (New-Object System.Drawing.RectangleF 0, 130, 164, 30), $sf)

$fontSub = New-Object System.Drawing.Font('Segoe UI', 9, [System.Drawing.FontStyle]::Regular)
$mutedBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(148, 163, 184))
$g.DrawString('Enterprise v1.0.0', $fontSub, $mutedBrush, (New-Object System.Drawing.RectangleF 0, 152, 164, 22), $sf)

# Decorative line
$accentPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(66, 133, 244), 2)
$g.DrawLine($accentPen, 35, 185, 129, 185)

# Bottom text
$fontSmall = New-Object System.Drawing.Font('Segoe UI', 7.5, [System.Drawing.FontStyle]::Regular)
$dimBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(100, 116, 139))
$g.DrawString('DPDP Act 2023', $fontSmall, $dimBrush, (New-Object System.Drawing.RectangleF 0, 265, 164, 18), $sf)
$g.DrawString('Compliance Platform', $fontSmall, $dimBrush, (New-Object System.Drawing.RectangleF 0, 280, 164, 18), $sf)

$g.Dispose()
$wiz.Save("d:\N_DPDP\QS-DPDP-Enterprise\installer-assets\wizard.bmp", [System.Drawing.Imaging.ImageFormat]::Bmp)
Write-Host "Wizard BMP saved"

# ═══════ Wizard Small Image (55x58) ═══════
Write-Host "Creating wizard small icon..."
$sm = New-Object System.Drawing.Bitmap 55, 58
$gs = [System.Drawing.Graphics]::FromImage($sm)
$gs.SmoothingMode = 'HighQuality'
$gs.InterpolationMode = 'HighQualityBicubic'
$gs.Clear([System.Drawing.Color]::White)
$gs.DrawImage($icon256, 2, 2, 51, 54)
$gs.Dispose()
$sm.Save("d:\N_DPDP\QS-DPDP-Enterprise\installer-assets\wizard-small.bmp", [System.Drawing.Imaging.ImageFormat]::Bmp)
Write-Host "Small icon saved"

$icon256.Dispose()
$miniIcon.Dispose()
$wiz.Dispose()
$sm.Dispose()

Write-Host "`nALL GOOGLE-STYLE ASSETS CREATED SUCCESSFULLY"
