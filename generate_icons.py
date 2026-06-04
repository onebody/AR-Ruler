from PIL import Image, ImageDraw, ImageOps
import os

def create_qiyun_icon(size):
    """创建骑云风格图标"""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    center = size // 2
    
    # 圆形背景 - 天蓝色
    bg_radius = int(size * 0.48)
    draw.ellipse([center - bg_radius, center - bg_radius,
                  center + bg_radius, center + bg_radius],
                 fill=(79, 195, 247))
    
    # 云朵
    cloud_y = center + int(size * 0.15)
    cloud_size = int(size * 0.38)
    draw.ellipse([center - cloud_size, cloud_y - cloud_size*0.5,
                  center + cloud_size, cloud_y + cloud_size*0.5], fill=(255,255,255))
    draw.ellipse([center - cloud_size*1.2, cloud_y - cloud_size*0.3,
                  center - cloud_size*0.5, cloud_y + cloud_size*0.3], fill=(255,255,255))
    draw.ellipse([center + cloud_size*0.5, cloud_y - cloud_size*0.3,
                  center + cloud_size*1.2, cloud_y + cloud_size*0.3], fill=(255,255,255))
    draw.ellipse([center - cloud_size*0.3, cloud_y - cloud_size*0.8,
                  center + cloud_size*0.3, cloud_y - cloud_size*0.2], fill=(255,255,255))
    
    # 人物头部
    face_y = center - int(size * 0.12)
    face_radius = int(size * 0.2)
    draw.ellipse([center - face_radius, face_y - face_radius,
                  center + face_radius, face_y + face_radius], fill=(255,218,193))
    
    # 头发
    hair_top = face_y - face_radius
    hair_points = [
        (center - face_radius*1.1, hair_top + int(size*0.05)),
        (center - face_radius*0.8, hair_top - int(size*0.1)),
        (center - face_radius*0.4, hair_top - int(size*0.15)),
        (center, hair_top - int(size*0.18)),
        (center + face_radius*0.4, hair_top - int(size*0.15)),
        (center + face_radius*0.8, hair_top - int(size*0.1)),
        (center + face_radius*1.1, hair_top + int(size*0.05)),
        (center + face_radius*1.1, hair_top + int(size*0.15)),
        (center - face_radius*1.1, hair_top + int(size*0.15)),
    ]
    draw.polygon(hair_points, fill=(30, 30, 30))
    
    # 眼睛
    eye_y = face_y - int(size*0.03)
    eye_size = int(size*0.055)
    draw.ellipse([center - int(size*0.09) - eye_size, eye_y - eye_size,
                  center - int(size*0.09) + eye_size, eye_y + eye_size], fill=(50,50,80))
    draw.ellipse([center - int(size*0.09) - eye_size*0.5, eye_y - eye_size*0.5,
                  center - int(size*0.09) + eye_size*0.5, eye_y + eye_size*0.5], fill=(255,255,255))
    draw.ellipse([center + int(size*0.09) - eye_size, eye_y - eye_size,
                  center + int(size*0.09) + eye_size, eye_y + eye_size], fill=(50,50,80))
    draw.ellipse([center + int(size*0.09) - eye_size*0.5, eye_y - eye_size*0.5,
                  center + int(size*0.09) + eye_size*0.5, eye_y + eye_size*0.5], fill=(255,255,255))
    
    # 嘴巴
    mouth_y = face_y + int(size*0.06)
    draw.ellipse([center - int(size*0.09), mouth_y - int(size*0.03),
                  center + int(size*0.09), mouth_y + int(size*0.05)], fill=(255,100,100))
    
    # 骑云文字
    text_y = center + int(size * 0.3)
    char_w = int(size * 0.11)
    char_h = int(size * 0.17)
    
    # 骑
    qi_x = center - int(size * 0.17)
    draw.rectangle([qi_x, text_y, qi_x + char_w*0.3, text_y + char_h], fill=(30,30,80))
    draw.rectangle([qi_x + char_w*0.4, text_y, qi_x + char_w*0.7, text_y + char_h*0.4], fill=(30,30,80))
    draw.rectangle([qi_x + char_w*0.4, text_y + char_h*0.5, qi_x + char_w*0.7, text_y + char_h], fill=(30,30,80))
    draw.rectangle([qi_x + char_w*0.8, text_y + char_h*0.3, qi_x + char_w, text_y + char_h], fill=(30,30,80))
    
    # 云
    yun_x = center + int(size * 0.06)
    draw.rectangle([yun_x, text_y, yun_x + char_w, text_y + char_h*0.2], fill=(30,30,80))
    draw.rectangle([yun_x, text_y + char_h*0.25, yun_x + char_w*0.2, text_y + char_h*0.6], fill=(30,30,80))
    draw.rectangle([yun_x + char_w*0.4, text_y + char_h*0.25, yun_x + char_w*0.6, text_y + char_h*0.6], fill=(30,30,80))
    draw.rectangle([yun_x + char_w*0.8, text_y + char_h*0.25, yun_x + char_w, text_y + char_h*0.6], fill=(30,30,80))
    draw.rectangle([yun_x, text_y + char_h*0.65, yun_x + char_w*0.3, text_y + char_h*0.85], fill=(30,30,80))
    draw.rectangle([yun_x + char_w*0.3, text_y + char_h*0.85, yun_x + char_w*0.6, text_y + char_h], fill=(30,30,80))
    draw.rectangle([yun_x + char_w*0.6, text_y + char_h*0.65, yun_x + char_w, text_y + char_h*0.85], fill=(30,30,80))
    
    return img

def main():
    densities = [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)]
    base_path = '/Users/fcj/workspace/Github_space/AR-Ruler/android/app/src/main/res'
    
    for density, size in densities:
        icon = create_qiyun_icon(size)
        density_path = os.path.join(base_path, f'mipmap-{density}')
        os.makedirs(density_path, exist_ok=True)
        
        icon.save(os.path.join(density_path, 'ic_launcher.png'))
        icon.save(os.path.join(density_path, 'ic_launcher_round.png'))
        icon.save(os.path.join(density_path, 'ic_launcher_foreground.png'))
        
        print(f"Created {density} icons ({size}x{size})")
    
    print("\nAll icons created successfully!")

if __name__ == '__main__':
    main()
