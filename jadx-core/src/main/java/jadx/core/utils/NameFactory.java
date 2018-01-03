package jadx.core.utils;

import java.util.ArrayList;
import java.util.List;

public class NameFactory {
    private static final int CHARACTER_COUNT = 26;

    private static char charAt(int index) {
        return (char) ((index < CHARACTER_COUNT ? 'a' :
                'A' - CHARACTER_COUNT) + index);
    }

    private static final List<String> cachedLowerCaseNames = new ArrayList<String>();

    private static String name(int index) {
        // Which cache do we need?
        List<String> cachedNames =
                cachedLowerCaseNames;

        // Do we have the name in the cache?
        if (index < cachedNames.size()) {
            return cachedNames.get(index);
        }

        // Create a new name and cache it.
        String name = newName(index);
        cachedNames.add(index, name);

        return name;
    }


    private static String newName(int index) {
        // If we're allowed to generate mixed-case names, we can use twice as
        // many characters.
        int totalCharacterCount = CHARACTER_COUNT;

        int baseIndex = index / totalCharacterCount;
        int offset = index % totalCharacterCount;

        char newChar = charAt(offset);

        return baseIndex == 0 ?
                new String(new char[]{newChar}) :
                (name(baseIndex - 1) + newChar);
    }

    // Implementations for NameFactory.
    private static int index = 0;

    public static void reset() {
        index = 0;
    }


    public static String nextName() {
        return name(index++);
    }
}
