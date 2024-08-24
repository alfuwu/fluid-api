# Usage
To use `fluid-api`, you can call the `FluidBuilder.create()` method.

However, calling this constructor method alone is not enough to create a full fluid, as there are many assets that are required on the client side to render a fluid.

Notably you will need `assets/<mod id>/blockstates/<fluid id>.json`, `assets/<mod id>/models/block/<fluid id>.json`, `assets/<mod id>/textures/<fluid id>_flow.png`, `assets/<mod id>/textures/<fluid id>_still.png`, and `assets/<mod id>/textures/<fluid id>_overlay.png`.

Additionally, if not disabled when creating the fluid using `FluidBuilder.create()`, the mod will automatically register a cauldron variant and bucket item, which require yet more files; `assets/<mod id>/blockstates/<fluid id>_cauldron.json`, `assets/<mod id>/models/block/<mod id>_cauldron.json`, `assets/<mod id>/models/item/<fluid id>_bucket.json` and wherever you specify the textures for the models of the cauldron and bucket. You may simply prevent the API from registering cauldrons and buckets by calling the `noCauldron()` and `noBucket()` methods when building a custom fluid, however.

It should be noted that infinite fluids are not working correctly.