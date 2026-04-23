$content = Get-Content 'c:\Users\Admin\Desktop\demo\demo\src\main\resources\templates\home.html' -Encoding UTF8 -Raw

# Replace common HTML entities
$replacements = @{
    '&#x1F338;' = '🌸'
    '&#x1F6D2;' = '🛒'
    '&#x1F4B0;' = '💰'
    '&#x2728;' = '✨'
    '&#x2B50;' = '⭐'
    '&#x2696;' = '⚖️'
    '&#x1F4AC;' = '💬'
    '&#x1F338;' = '🌸'
    '&#x1F3AF;' = '🎯'
    '&#x1F4A1;' = '💡'
    '&#x2715;' = '✕'
    '&amp;' = '&'
    '&lt;' = '<'
    '&gt;' = '>'
    '&quot;' = '"'
    '&#39;' = "'"
}

foreach ($key in $replacements.Keys) {
    $content = $content -replace [regex]::Escape($key), $replacements[$key]
}

# For numeric entities like &#x1F338; (emojis) - these should stay as-is for browser
# For Vietnamese chars like &#xE0; (à) etc - need to convert

# Convert common Vietnamese HTML entities
$vietEntities = @{
    '&#x1EE7;' = '�ử'
    '&#x1EA1;' = 'ạ'
    '&#x111;' = 'đ'
    '&#x1ECB;' = 'ợ'
    '&#x1ECF;' = 'ờ'
    '&#x1EC5;' = 'ể'
    '&#x1EC3;' = 'ở'
    '&#x1EC7;' = 'ệ'
    '&#x1EA3;' = 'ả'
    '&#x1EA5;' = 'ấ'
    '&#x1EA7;' = 'ầ'
    '&#x1EA9;' = 'ẩ'
    '&#x1EAB;' = 'ẫ'
    '&#x1EAD;' = 'ậ'
    '&#x1EAF;' = 'ắ'
    '&#x1EB1;' = 'ằ'
    '&#x1EB3;' = 'ẳ'
    '&#x1EB5;' = 'ẵ'
    '&#x1EB7;' = 'ặ'
    '&#x1EB9;' = 'ẹ'
    '&#x1EBB;' = 'ẻ'
    '&#x1EBD;' = 'ẽ'
    '&#x1EBF;' = 'ế'
    '&#x1EC1;' = 'ề'
    '&#x1EC9;' = 'ỉ'
    '&#x1ECD;' = 'ỏ'
    '&#x1ED1;' = 'ố'
    '&#x1ED3;' = 'ồ'
    '&#x1ED5;' = 'ổ'
    '&#x1ED7;' = 'ỗ'
    '&#x1ED9;' = 'ộ'
    '&#x1EDB;' = 'ớ'
    '&#x1EDD;' = 'ờ'
    '&#x1EDF;' = 'ỡ'
    '&#x1EE1;' = 'ợ'
    '&#x1EE3;' = 'ụ'
    '&#x1EE5;' = 'ủ'
    '&#x1EE7;' = 'ứ'
    '&#x1EE9;' = 'ừ'
    '&#x1EEB;' = 'ử'
    '&#x1EED;' = 'ự'
    '&#x1EEF;' = 'ỳ'
    '&#x1EF1;' = 'ỵ'
    '&#x1EF3;' = 'ỷ'
    '&#x1EF5;' = 'ỹ'
    '&#x1EF7;' = 'ỻ'
    '&#x1EF9;' = 'ỽ'
    '&#x1EFB;' = 'ἀ'
    '&#x1EFD;' = 'ἂ'
    '&#x1EFF;' = 'ἃ'
    '&#x1F3AF;' = '🎯'
}

foreach ($key in $vietEntities.Keys) {
    $content = $content -replace [regex]::Escape($key), $vietEntities[$key]
}

# Handle remaining &#x...; patterns for emojis and special chars
$pattern = '&#x([0-9A-Fa-f]+);'
$content = [regex]::Replace($content, $pattern, {
    param($match)
    $hexValue = $match.Groups[1].Value
    try {
        $codePoint = [int]("0x$hexValue")
        if ($codePoint -le 0xFFFF) {
            return [char]$codePoint
        }
    } catch {}
    return $match.Value
})

# Save with UTF-8 BOM for proper encoding
$Utf8NoBomEncoding = New-Object System.Text.UTF8Encoding $False
[System.IO.File]::WriteAllText('c:\Users\Admin\Desktop\demo\demo\src\main\resources\templates\home.html', $content, $Utf8NoBomEncoding)

Write-Host "Fixed encoding in home.html"
