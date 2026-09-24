# Rise 1.1 visual assets

All 18 exercise entries have local images and a direct YouTube tutorial. The 14 distinct tutorial IDs, image mappings, creator names and adaptation notes are in `media.json`. YouTube's oEmbed endpoint returned matching video titles and creators for all 14 IDs on 18 September 2026. This verifies metadata and availability, not complete playback in every country. Videos open in the user's YouTube app or browser; internet is required.

Exercise photos come from https://github.com/yuhonas/free-exercise-db under the Unlicense (included in exercises-LICENSE.md). Some source photos show gym equipment; captions explain the home variation. No supplied personal photos are included.

The image-generation tool created nine original images for this app, saved unchanged in app/src/main/assets/rise_visuals. Food images are illustrative; use written portions. Exercise drawings are form references, accompanied by written cues and creator tutorials.

Generation brief: consistent dark green, natural-light Indian food photography for protein choices, idli/sambar/egg breakfast, rice/chicken/vegetable/curd lunch, chapati/soy/dal dinner, and milk/chana/curd/guava snacks. Exercise illustrations show a home dumbbell Romanian deadlift, wall-supported split squat with both feet on the floor, knees-down side plank, and single-arm dumbbell floor press. Avoid logos, captions and decorative text; keep the whole body visible.

Images use native Android ImageViews, accessible descriptions, tap-to-enlarge and bounded bitmap decoding with a 12 MiB cache. The app has no Internet permission; media is bundled offline. YouTube links launch externally.
