const fs = require('fs');
// fetch is global

async function run() {
    try {
        // Just extract a frame from video using ffmpeg or dummy image if easy.
        // Wait, we don't need ffmpeg, we can just use a dummy tiny jpeg to test the route!
        // Or I can send random base64 string to see if it handles failure.
        // Let's create a tiny red JPEG:
        const tinyJpeg = Buffer.from('/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=', 'base64');
        
        const res = await fetch('http://localhost:3000/api/ml/predict-frame', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ image: tinyJpeg.toString('base64') })
        });
        const text = await res.text();
        console.log("RESULT:", text);
    } catch(err) {
        console.error(err);
    }
}
run();
