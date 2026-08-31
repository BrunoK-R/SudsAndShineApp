# Production automotive imagery

The pixel-parity pass uses four text-free raster assets generated with the built-in image-generation tool. The approved AI Home and booking concepts were supplied only as composition, palette, lighting, and mood references. The source generations remain in Codex's generated-image store; the project copies are 1200 × 800 JPEG masters at quality 90.

| Project resource | Purpose |
| --- | --- |
| `shared/.../drawable/suds_appointment_hero.jpg` | Upcoming appointment hero |
| `shared/.../drawable/suds_service_standard.jpg` | Standard/complete service |
| `shared/.../drawable/suds_service_premium.jpg` | Premium/detailing service |
| `shared/.../drawable/suds_service_exterior.jpg` | Exterior wash service |
| `shared/.../drawable/suds_brand_mark.png` | Existing approved Suds & Shine mark, resized for shared UI use |
| `feature/onboarding/.../drawable/suds_splash_mark.png` | User-selected car-wash mark, prepared on the shared splash navy |

## Splash mark

The splash uses the mark explicitly selected by the user from
`/Users/mafaldaribeiro/.codex/generated_images/01a05466-a1f9-7ae1-ad5c-4bde0a80b6ff/exec-815685e3-1fa8-4eba-a64e-e24532ec6d13.png`.
That source contained a checkerboard baked into an RGB image rather than a real alpha channel. A faithful image-generation edit was therefore made to preserve the white car, cyan foam, circular swooshes, and sparkles while replacing only the checkerboard with the app's `#142539` splash background. The resulting production copy was resized to 768 × 768 and its outer edge was blended to exactly `#142539`, preventing a visible bitmap boundary during the native-to-Compose splash handoff.

```text
Prepare the supplied Suds & Shine car-wash mark as a production splash-screen
bitmap. Preserve the supplied logo composition faithfully: the same white sports
car silhouette, deep navy linework, cyan foam bubbles, twin cyan/navy circular
swooshes, sparkle details, proportions, orientation, and centered scale. Replace
only the visible gray-and-white checkerboard with one perfectly uniform, flat,
solid deep navy background color RGB 20,37,57 / hex #142539, extending to every
edge. No checker pattern, texture, vignette, gradient, shadow, glow, panel, border,
text, or additional element. Keep the white car opaque bright white and all
cyan/navy details crisp. Square output with generous navy safe padding and no
cropped strokes.
```

## Final prompt set

### Appointment hero

```text
Use case: photorealistic-natural
Asset type: cross-platform mobile app appointment hero background
Input images: Image 1 is a composition, palette, lighting, and mood reference only
Primary request: create a premium cinematic photo of a generic pearl-white performance sedan being professionally washed, covered with fresh foam and water droplets, in a dark detailing studio
Composition/framing: wide landscape 16:10 crop; front-right three-quarter view of the car occupies the right 58 percent; the entire left 42 percent is clean dark navy negative space suitable for live UI text; keep the headlight and front wheel inside safe bounds; no important detail at the outer edges
Lighting/mood: dramatic controlled studio rim light, cool cyan reflections, rich deep navy shadows, polished and realistic rather than concept art
Materials/textures: convincing wet metallic paint, foam, glass, rubber, water mist and glossy floor
Color palette: near-black navy, steel blue, restrained electric cyan, white vehicle
Constraints: image only; no UI, no text, no badges, no icons, no logos, no brand marks, no license plate characters, no watermark; do not copy text or interface elements from Image 1
```

### Standard service

```text
Use case: photorealistic-natural
Asset type: cross-platform mobile app service-card background for Standard wash
Input images: Image 1 is a composition, palette, lighting, and mood reference only
Primary request: create a premium cinematic photo of a meticulously cleaned generic luxury car interior, showing black leather front seats, center console, steering wheel, dashboard, and subtle detailing sheen
Composition/framing: landscape 16:10 image designed for responsive cropping; visual interest and cabin detail concentrated on the right 62 percent; the left 38 percent remains dark, low-detail navy-black negative space for live UI text; no important detail at the outer edges
Lighting/mood: warm restrained cabin highlights balanced with cool deep-blue shadows, professional automotive editorial photography
Materials/textures: authentic leather grain, brushed metal, glass, subtle stitching, no synthetic 3D look
Color palette: near-black, charcoal, dark navy, small warm highlights
Constraints: image only; no people, no UI, no text, no badges, no icons, no logos, no brand marks, no readable screens, no watermark; do not copy interface elements from Image 1
```

### Premium service

```text
Use case: photorealistic-natural
Asset type: cross-platform mobile app service-card background for Premium wash
Input images: Image 1 is a composition, palette, lighting, and mood reference only
Primary request: create a premium cinematic photo of a generic glossy black luxury SUV immediately after a meticulous wash, front-right three-quarter angle, water beads on the body and glass, wet detailing-studio floor and subtle mist
Composition/framing: landscape 16:10 image designed for responsive cropping; headlight, grille, front wheel and wet body concentrated on the right 62 percent; the left 38 percent is dark, low-detail navy negative space for live UI text; preserve the complete headlight and wheel within safe bounds
Lighting/mood: dramatic cool cyan edge light, controlled specular reflections, deep navy shadows, premium automotive campaign photography, realistic rather than 3D
Materials/textures: wet black metallic paint, glass, tire rubber, water droplets, glossy concrete reflections
Color palette: near-black navy, black, steel blue, restrained electric cyan and silver highlights
Constraints: image only; no people, no UI, no text, no badges, no icons, no logos, no brand marks, no license plate characters, no watermark; do not copy interface elements from Image 1
```

### Exterior service

```text
Use case: photorealistic-natural
Asset type: cross-platform mobile app service-card background for Exterior wash
Input images: Image 1 is a composition, palette, lighting, and mood reference only
Primary request: create a premium close automotive-detailing photo of a black-gloved hand washing the wet hood and headlight of a generic black car with a dense natural sea sponge, clean foam, and crisp water beads
Composition/framing: landscape 16:10 image designed for responsive cropping; hand, sponge, headlight and wet body concentrated on the right 62 percent; the left 38 percent is dark, low-detail navy-black negative space for live UI text; keep the hand and headlight inside safe bounds
Lighting/mood: controlled professional detailing-bay light, cool cyan highlights, deep navy shadows, realistic editorial macro photography
Materials/textures: authentic wet clearcoat, microfiber/glove texture, porous sponge, foam and droplets
Color palette: black, near-black navy, steel blue, subtle cyan and white foam
Constraints: image only; no face or identifiable person, no UI, no text, no badges, no icons, no logos, no brand marks, no license plate, no watermark; do not copy interface elements from Image 1
```
