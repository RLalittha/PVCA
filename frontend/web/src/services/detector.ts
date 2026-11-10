import * as tf from '@tensorflow/tfjs';
import * as cocoSsd from '@tensorflow-models/coco-ssd';
import { DetectedObject } from '../types';

let modelPromise: Promise<cocoSsd.ObjectDetection> | null = null;

/**
 * Initializes and caches the COCO-SSD object detection model.
 * Uses the lightweight MobileNet backbone for fast CPU/browser inference.
 */
export async function getDetectorModel(): Promise<cocoSsd.ObjectDetection> {
  if (!modelPromise) {
    // Ensure TensorFlow.js backend is ready (WebGL or CPU)
    await tf.ready();
    modelPromise = cocoSsd.load({ base: 'lite_mobilenet_v2' });
  }
  return modelPromise;
}

/**
 * Maps COCO-80 object classes into PVCA commercial categories
 * recognized by the FastAPI matching engine.
 */
export function mapCocoClassToCategory(className: string): string {
  const c = className.toLowerCase();

  // Fashion & Wearables
  if (['backpack', 'handbag', 'suitcase', 'tie', 'umbrella'].includes(c)) {
    return 'Fashion good';
  }

  // Drinkware & Kitchenware
  if (['bottle', 'cup', 'wine glass', 'bowl'].includes(c)) {
    return 'Drinkware';
  }

  // Consumer Electronics
  if (['cell phone', 'laptop', 'mouse', 'remote', 'keyboard', 'tv', 'clock', 'microwave', 'toaster'].includes(c)) {
    return 'Consumer Electronics';
  }

  // Home Goods & Furniture
  if (['chair', 'couch', 'bed', 'dining table', 'potted plant', 'vase', 'refrigerator', 'sink'].includes(c)) {
    return 'Home good';
  }

  // Sports & Outdoors
  if (['sports ball', 'tennis racket', 'baseball glove', 'baseball bat', 'skateboard', 'surfboard', 'skis', 'snowboard', 'frisbee', 'kite'].includes(c)) {
    return 'Sports & Outdoors';
  }

  // Food & Perishables
  if (['banana', 'apple', 'sandwich', 'orange', 'broccoli', 'carrot', 'hot dog', 'pizza', 'donut', 'cake'].includes(c)) {
    return 'Food';
  }

  return 'General Object';
}

/**
 * Detects multiple objects in an HTML image element.
 * Returns sorted detections with real bounding boxes and category mappings.
 */
export async function detectObjects(imageElement: HTMLImageElement): Promise<DetectedObject[]> {
  const model = await getDetectorModel();
  const rawPredictions = await model.detect(imageElement, 8, 0.25);

  return rawPredictions.map((pred, index) => {
    const category = mapCocoClassToCategory(pred.class);
    return {
      id: index + 1,
      label: pred.class,
      score: pred.score,
      bbox: pred.bbox, // [x, y, width, height] in natural pixel coords
      category,
    };
  });
}
