# Usage
To use Fluid API, you can call the `FluidBuilder.create()` method.

However, calling this constructor method alone is not enough to create a full fluid, as there are many assets that are required on the client side to render a fluid.

Notably you will need `assets/<mod id>/blockstates/<fluid id>.json`, `assets/<mod id>/models/block/<fluid id>.json`, `assets/<mod id>/textures/<fluid id>_flow.png`, `assets/<mod id>/textures/<fluid id>_still.png`, and `assets/<mod id>/textures/<fluid id>_overlay.png`.

Additionally, if not disabled when creating the fluid using `FluidBuilder.create()`, the mod will automatically register a cauldron variant and bucket item, which require yet more files; `assets/<mod id>/blockstates/<fluid id>_cauldron.json`, `assets/<mod id>/models/block/<mod id>_cauldron.json`, `assets/<mod id>/models/item/<fluid id>_bucket.json` and wherever you specify the textures for the models of the cauldron and bucket. You may simply prevent the API from registering cauldrons and buckets by calling the `noCauldron()` and `noBucket()` methods when building a custom fluid, however. The cauldron will also need a `loot_table` so that, when mined in survival mode, it drops the cauldron item. The JSON file location for the loot table will be at `data/<mod id>/loot_tables/blocks/<fluid id>_cauldron.json`. Furthermore, the cauldron will need to be added to the `#cauldrons` tag so that pickaxes are required to mine the cauldron properly. The file location for the `#cauldrons` tag is located at `data/minecraft/tags/blocks/cauldrons.json`.

It should be noted that infinite fluids are not working correctly.

## Adding Fluid API as a project dependency
Fluid API is built using JitPack, so to use it you will need to add JitPack to your `build.gradle`'s repositories, like so:
```groovy
repositories {
	// ...
	maven { url 'https://jitpack.io' }
	// ...
}
```

Afterwards, you will be able to add `modImplementation "com.github.alfuwu:fluid-api:master-SNAPSHOT"` to the dependencies section of your `build.gradle` file. Note that `master-SNAPSHOT` can also be replaced with any of the Fluid API's built versions.

## Leveled Cauldrons
By default, Fluid API will register a custom cauldron for your fluid. To disable this behavior, you may call the `noCauldron()` method while building your fluid. However, if `noCauldron()` is not called, there are many files that need to be added to your assets folder.
The Fluid API contains an example of this, and may be used as a reference.
If you registered a bottle along with your fluid, the Fluid API will create a leveled cauldron (similar to that of the vanilla water cauldron) which requires three model files, one for each level. These files located at `assets/<mod id>/models/block/<fluid id>_cauldron_level1.json`, `assets/<mod id>/models/block/<fluid id>_cauldron_level2.json`, and `assets/<mod id>/models/block/<fluid id>_cauldron_full.json`. The full model is exactly the same as the model you would use if you did not register a bottle item. Finally, if you are using a leveled cauldron, you will need to modify `assets/<mod id>/blockstats/<fluid id>_cauldron.json` file. For a "normal" cauldron, the file should look something like this:
```json
{
  "variants": {
    "": {
      "model": "mod-id:block/fluid_cauldron"
    }
  }
}
```
In the above file, the cauldron only has one level, and that level is the completely full level. However, for leveled cauldrons, we need to specify a model file for each level of the cauldron. A leveled cauldron's blockstate file should look like this:
```json
{
  "variants": {
    "level=1": {
      "model": "mod-id:block/fluid_cauldron_level1"
    },
    "level=2": {
      "model": "mod-id:block/fluid_cauldron_level2"
    },
    "level=3": {
      "model": "mod-id:block/fluid_cauldron_full"
    }
  }
}
```

## Bottles
When you register a bottle using the `bottle()` method while constructing a fluid, the mod will register three different items, `<fluid id>_bottle`, `splash_<fluid id>_bottle`, and `lingering_<fluid id>_bottle`. You may remove any of these three by simply overriding their factory method and setting it to null using the `customBottleItem()` method, et cetera. You will need to add assets to your assets folder for these items to properly render in-game.