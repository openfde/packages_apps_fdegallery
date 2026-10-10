package com.fde.gallery3d.ui.ai;

import androidx.annotation.Nullable;

import com.fde.gallery3d.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * AI 图片自动分类：人像 / 风景 / 动物 / 美食 / 植物 / 建筑 / 车辆。
 * 每个分类对应一组 ImageNet 标签，分类器给出顶层标签后据此归类。
 */
public enum AiCategory {

    PORTRAIT("portrait", R.string.category_portrait),
    LANDSCAPE("landscape", R.string.category_landscape),
    ANIMAL("animal", R.string.category_animal),
    FOOD("food", R.string.category_food),
    PLANT("plant", R.string.category_plant),
    ARCHITECTURE("architecture", R.string.category_architecture),
    VEHICLE("vehicle", R.string.category_vehicle),
    OTHER("other", R.string.category_other);

    private final String id;
    private final int labelRes;

    private static final Map<String, AiCategory> MATCH_MAP = new HashMap<>();

    AiCategory(String id, int labelRes) {
        this.id = id;
        this.labelRes = labelRes;
    }

    public String id() {
        return id;
    }

    public int labelRes() {
        return labelRes;
    }

    /**
     * 由分类器输出标签（ImageNet label）映射到图库分类。
     */
    public static AiCategory fromLabel(String label) {
        if (label == null) {
            return OTHER;
        }
        AiCategory category = MATCH_MAP.get(label.trim().toLowerCase(Locale.ROOT));
        return category == null ? OTHER : category;
    }

    /**
     * 由存储用的字符串（枚举名或 id）得到分类，无法识别时返回 OTHER。
     */
    public static AiCategory fromId(@Nullable String value) {
        if (value == null) {
            return OTHER;
        }
        for (AiCategory category : values()) {
            if (category.name().equals(value) || category.id.equalsIgnoreCase(value)) {
                return category;
            }
        }
        return OTHER;
    }

    private static final String[] PORTRAIT_LABELS = {
            "academic gown", "apron", "bathing cap", "bikini", "cardigan", "clog",
            "cowboy boot", "cowboy hat", "crash helmet", "football helmet", "fur coat",
            "gown", "jersey", "kimono", "lipstick", "maillot", "military uniform",
            "miniskirt", "poncho", "sandal", "sarong", "sombrero", "sunglass",
            "sunglasses", "sweatshirt", "trench coat", "wig", "groom", "suit",
    };

    private static final String[] LANDSCAPE_LABELS = {
            "alp", "breakwater", "coral reef", "cliff", "dock", "geyser", "lakeside",
            "promontory", "sandbar", "seashore", "valley", "volcano",
    };

    private static final String[] ANIMAL_LABELS = {
            "tench", "goldfish", "great white shark", "tiger shark", "electric ray",
            "stingray", "eel", "anemone fish", "sturgeon", "gar", "lionfish", "puffer",
            "grey whale", "killer whale", "dugong", "sea lion", "sea anemone",
            "sea urchin", "jellyfish", "starfish", "Dungeness crab", "rock crab",
            "fiddler crab", "king crab", "American lobster", "spiny lobster",
            "hermit crab", "snail", "slug", "sea slug",
            "hen", "ostrich", "goldfinch", "house finch", "robin", "jay", "magpie",
            "bald eagle", "vulture", "great grey owl", "prairie chicken", "peacock",
            "quail", "partridge", "macaw", "sulphur-crested cockatoo", "lorikeet",
            "bee eater", "hummingbird", "toucan", "goose", "black swan", "flamingo",
            "white stork", "black stork", "little blue heron", "American egret",
            "bustard", "pelican", "king penguin", "albatross", "crane", "bittern",
            "red-backed sandpiper", "oystercatcher", "ringlet",
            "European fire salamander", "common newt", "spotted salamander",
            "bullfrog", "tree frog", "tailed frog", "leatherback turtle", "mud turtle",
            "box turtle", "banded gecko", "common iguana", "American chameleon",
            "frilled lizard", "alligator lizard", "Gila monster", "green lizard",
            "African chameleon", "African crocodile", "American alligator", "triceratops",
            "thunder snake", "ringneck snake", "hognose snake", "green snake",
            "king snake", "garter snake", "water snake", "vine snake", "night snake",
            "sea snake",
            "scorpion", "black and gold garden spider", "barn spider", "garden spider",
            "wolf spider", "tick", "tiger beetle", "ladybug", "ground beetle",
            "long-horned beetle", "leaf beetle", "dung beetle", "rhinoceros beetle",
            "fly", "bee", "ant", "grasshopper", "cricket", "dragonfly", "damselfly",
            "cabbage butterfly", "sulphur butterfly", "mantis",
            "Maltese dog", "wire-haired fox terrier", "Greater Swiss Mountain dog",
            "Bernese mountain dog", "Eskimo dog", "timber wolf", "white wolf", "red wolf",
            "African hunting dog", "red fox", "kit fox", "Arctic fox", "grey fox",
            "tiger cat", "Persian cat", "Siamese cat", "Egyptian cat",
            "leopard", "snow leopard", "lion", "tiger", "cheetah", "brown bear",
            "American black bear", "ice bear", "sloth bear", "mongoose", "wood rabbit",
            "hare", "hamster", "porcupine", "fox squirrel", "beaver", "guinea pig",
            "zebra", "wild boar", "hippopotamus", "ox", "water buffalo", "bison",
            "gazelle", "hartebeest", "Arabian camel", "llama", "weasel", "mink",
            "black-footed ferret", "otter", "skunk", "badger", "armadillo",
            "three-toed sloth", "orangutan", "gorilla", "chimpanzee", "gibbon",
            "siamang", "baboon", "colobus", "proboscis monkey", "marmoset",
            "howler monkey", "spider monkey", "squirrel monkey", "Madagascar cat",
            "Indian elephant", "African elephant", "giant panda", "koala", "wallaby",
            "wombat", "platypus", "echidna", "mouse", "basset", "beagle", "collie",
            "Great Dane", "Saint Bernard", "pug", "Pembroke", "kelpie", "komondor",
            "jaguar", "lynx", "coyote", "hyena",
    };

    private static final String[] FOOD_LABELS = {
            "plate", "guacamole", "consomme", "hot pot", "trifle", "ice cream",
            "ice lolly", "French loaf", "bagel", "pretzel", "cheeseburger", "hotdog",
            "mashed potato", "head cabbage", "broccoli", "cauliflower", "zucchini",
            "spaghetti squash", "acorn squash", "butternut squash", "cucumber",
            "artichoke", "bell pepper", "mushroom", "Granny Smith", "strawberry",
            "orange", "lemon", "fig", "pineapple", "banana", "jackfruit",
            "custard apple", "pomegranate", "carbonara", "chocolate sauce", "dough",
            "meat loaf", "pizza", "potpie", "burrito", "red wine", "espresso",
            "eggnog", "soup bowl", "corn",
    };

    private static final String[] PLANT_LABELS = {
            "daisy", "yellow lady's slipper", "hip", "sorrel", "coral fungus",
            "agaric", "gyromitra", "stinkhorn", "earthstar", "hen-of-the-woods",
            "bolete",
    };

    private static final String[] ARCHITECTURE_LABELS = {
            "castle", "church", "cliff dwelling", "dome", "monastery", "mosque",
            "obelisk", "palace", "steel arch bridge", "suspension bridge", "viaduct",
            "water tower",
    };

    private static final String[] VEHICLE_LABELS = {
            "aircraft carrier", "airliner", "airship", "ambulance", "beach wagon",
            "bicycle-built-for-two", "bullet train", "canoe", "catamaran",
            "container ship", "convertible", "electric locomotive", "fire engine",
            "fireboat", "forklift", "freight car", "garbage truck", "golfcart",
            "gondola", "horse cart", "jeep", "lawn mower", "limousine", "liner",
            "minibus", "minivan", "moped", "motor scooter", "mountain bike", "oxcart",
            "passenger car", "police van", "school bus", "snowmobile", "speedboat",
            "sports car", "steam locomotive", "streetcar", "tank", "tow truck",
            "tractor", "trailer truck", "tricycle", "trolleybus", "unicycle",
            "warplane",
    };

    static {
        put(PORTRAIT, PORTRAIT_LABELS);
        put(LANDSCAPE, LANDSCAPE_LABELS);
        put(ANIMAL, ANIMAL_LABELS);
        put(FOOD, FOOD_LABELS);
        put(PLANT, PLANT_LABELS);
        put(ARCHITECTURE, ARCHITECTURE_LABELS);
        put(VEHICLE, VEHICLE_LABELS);
    }

    private static void put(AiCategory category, String[] labels) {
        if (labels == null) {
            return;
        }
        for (String label : labels) {
            if (label != null) {
                MATCH_MAP.put(label.toLowerCase(Locale.ROOT), category);
            }
        }
    }
}
