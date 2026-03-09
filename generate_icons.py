import os
from PIL import Image
import sys

def generate_icons(source_image_path, res_dir):
    try:
        img = Image.open(source_image_path).convert("RGBA")
    except Exception as e:
        print(f"Error opening image: {e}")
        return

    # Standard legacy icons
    sizes = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192
    }

    # Adaptive icon foregrounds (108x108 dp)
    # The actual pixel sizes for adaptive icon foregrounds:
    # mdpi: 108x108
    # hdpi: 162x162
    # xhdpi: 216x216
    # xxhdpi: 324x324
    # xxxhdpi: 432x432
    adaptive_sizes = {
        "mdpi": 108,
        "hdpi": 162,
        "xhdpi": 216,
        "xxhdpi": 324,
        "xxxhdpi": 432
    }

    for density, size in sizes.items():
        folder = os.path.join(res_dir, f"mipmap-{density}")
        os.makedirs(folder, exist_ok=True)
        
        # Legacy icon
        legacy_icon = img.resize((size, size), Image.Resampling.LANCZOS)
        legacy_icon.save(os.path.join(folder, "ic_launcher.png"))
        
        # Legacy round icon
        legacy_icon.save(os.path.join(folder, "ic_launcher_round.png"))

    for density, size in adaptive_sizes.items():
        folder = os.path.join(res_dir, f"mipmap-{density}")
        os.makedirs(folder, exist_ok=True)
        
        # Adaptive foreground
        fg_icon = img.resize((size, size), Image.Resampling.LANCZOS)
        fg_icon.save(os.path.join(folder, "ic_launcher_foreground.png"))
        
    print("Icons generated successfully!")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python generate_icons.py <source_image> <res_dir>")
        sys.exit(1)
    generate_icons(sys.argv[1], sys.argv[2])
