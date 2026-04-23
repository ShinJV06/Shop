# -*- coding: utf-8 -*-
import html

with open(r'c:\Users\Admin\Desktop\demo\demo\src\main\resources\templates\home.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Decode HTML entities
decoded = html.unescape(content)

with open(r'c:\Users\Admin\Desktop\demo\demo\src\main\resources\templates\home.html', 'w', encoding='utf-8') as f:
    f.write(decoded)

print("Fixed encoding in home.html")
