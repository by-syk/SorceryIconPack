package com.sorcerer.sorcery.iconpack;

import com.sorcerer.sorcery.iconpack.net.spiders.AppNameGetter;
import com.sorcerer.sorcery.iconpack.net.spiders.AppSearchResultGetter;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import io.reactivex.functions.Consumer;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        AppSearchResultGetter.search("settings")
                .subscribe(strings -> {
                    System.out.println(Arrays.toString(strings.toArray()).replace(",", "\n"));
                });
    }

    @Test
    public void appNameGetterTest() throws Exception {
        AppNameGetter.getName("com.tiantonglaw.readlaw")
                .subscribe(System.out::println, Throwable::printStackTrace);
    }
}