import cv2
import base64
import requests
import os
import time

def test_camera(path, cam_id, ms):
    cap = cv2.VideoCapture(path)
    cap.set(cv2.CAP_PROP_POS_MSEC, ms)
    ret, frame = cap.read()
    if not ret:
        print(f"Failed to read frame from {path}")
        return
    
    start = time.time()
    _, buffer = cv2.imencode('.jpg', frame, [int(cv2.IMWRITE_JPEG_QUALITY), 90])
    b64 = base64.b64encode(buffer).decode('utf-8')
    
    print(f"Frame Size: {len(buffer)} bytes (Base64: {len(b64)} chars)")
    print(f"Image Dimensions: {frame.shape[1]}x{frame.shape[0]}")
    
    req_start = time.time()
    res = requests.post('http://localhost:3000/api/ml/predict-frame', json={'imageBase64': b64})
    elapsed = int((time.time() - req_start) * 1000)
    print(f"HTTP Status: {res.status_code}")
    print(f"Elapsed: {elapsed}ms")
    print(f"RAW RES: {res.text}")

base_dir = r"c:\Users\piyus\Downloads\Drishti360\backend\media"
test_camera(os.path.join(base_dir, "cctv_demo_2.mp4"), "Cam 02", 5000)
