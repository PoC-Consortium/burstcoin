package brs.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static brs.Constants.FEE_QUANT;

public class FeeSuggestionTest {

    @Test
    public void testSimple() {
        List<List<Long>> fees = Arrays.asList(Arrays.asList(FEE_QUANT, 2 * FEE_QUANT, 4 * FEE_QUANT));

        Assert.assertEquals(3 * FEE_QUANT, FeeSuggestion.suggestFee(fees, 0.5, 1020));
    }

    @Test
    public void test() {
        List<List<Long>> fees = Arrays.asList(
                Arrays.asList(FEE_QUANT, 2 * FEE_QUANT, 3 * FEE_QUANT),
                Arrays.asList(FEE_QUANT, 2 * FEE_QUANT, 3 * FEE_QUANT),
                Arrays.asList(FEE_QUANT, 2 * FEE_QUANT, 3 * FEE_QUANT, 4 * FEE_QUANT),
                Arrays.asList(FEE_QUANT, 2 * FEE_QUANT, 3 * FEE_QUANT, 4 * FEE_QUANT)
        );

        Assert.assertEquals(4 * FEE_QUANT, FeeSuggestion.suggestFee(fees, 0.5, 1020));
        Assert.assertEquals(5 * FEE_QUANT, FeeSuggestion.suggestFee(fees, 0.7, 1020));
    }

    @Test
    public void testFull() {
        List<List<Long>> fees = Arrays.asList(
                Arrays.asList(FEE_QUANT, 2 * FEE_QUANT, 3 * FEE_QUANT),
                Arrays.asList(FEE_QUANT, 2 * FEE_QUANT, 3 * FEE_QUANT),
                Arrays.asList(FEE_QUANT, 2 * FEE_QUANT, 3 * FEE_QUANT),
                Arrays.asList(FEE_QUANT, 2 * FEE_QUANT, 3 * FEE_QUANT)
        );

        Assert.assertEquals(3 * FEE_QUANT, FeeSuggestion.suggestFee(fees, 0.5, 3));
        Assert.assertEquals(3 * FEE_QUANT, FeeSuggestion.suggestFee(fees, 0.7, 3));
    }

    @Test
    public void testEmpty() {
        List<List<Long>> fees = Arrays.asList(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        Assert.assertEquals(FEE_QUANT, FeeSuggestion.suggestFee(fees, 0.5, 3));
        Assert.assertEquals(FEE_QUANT, FeeSuggestion.suggestFee(fees, 0.7, 3));
    }
}
