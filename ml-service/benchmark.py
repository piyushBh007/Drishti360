import time
import cv2
import glob
from ultralytics import YOLO

# Configurations to test
configs = [
    {"model": "yolov8n.pt", "conf": 0.40, "imgsz": 640, "iou": 0.7, "max_det": 300},
    {"model": "yolov8n.pt", "conf": 0.20, "imgsz": 960, "iou": 0.5, "max_det": 100},
    {"model": "yolov8n.pt", "conf": 0.20, "imgsz": 1280, "iou": 0.5, "max_det": 100},
    {"model": "yolov8s.pt", "conf": 0.20, "imgsz": 960, "iou": 0.5, "max_det": 100},
    {"model": "yolov8s.pt", "conf": 0.20, "imgsz": 1280, "iou": 0.5, "max_det": 100},
]

frames = glob.glob('benchmark_frames/*.jpg')
for frame_path in frames:
    print(f"\nEvaluating frame: {frame_path}")
    frame = cv2.imread(frame_path)
    
    for cfg in configs:
        model = YOLO(cfg['model'])
        start_time = time.time()
        results = model(frame, conf=cfg['conf'], imgsz=cfg['imgsz'], iou=cfg['iou'], max_det=cfg['max_det'], classes=[0], verbose=False)
        inference_time = (time.time() - start_time) * 1000
        
        persons = 0
        boxes = results[0].boxes
        if boxes is not None:
            for box in boxes:
                cls_id = int(box.cls[0].item())
                if cls_id == 0:
                    persons += 1
                    
        print(f"Model: {cfg['model']:12}, Conf: {cfg['conf']:.2f}, imgsz: {cfg['imgsz']:4}, iou: {cfg['iou']:.2f} -> Persons: {persons:2}, Time: {inference_time:.1f}ms")
