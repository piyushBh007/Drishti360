from services.yolo_service import analyze_video
import os

video = r"C:\Drishti360\backend\media\cctv_demo.mp4"
if os.path.exists(video):
    res = analyze_video(video, confidence_threshold=0.15)
    print("DEMO 1 MAX:", res['maximumPersonCount'])

video2 = r"C:\Drishti360\backend\media\cctv_demo_2.mp4"
if os.path.exists(video2):
    res2 = analyze_video(video2, confidence_threshold=0.15)
    print("DEMO 2 MAX:", res2['maximumPersonCount'])
