package brs.util;

import brs.*;
import brs.db.BurstIterator;
import brs.fluxcapacitor.FluxInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static brs.Constants.*;

public class FeeSuggestion {

    public enum Priority {
        NORMAL, EXPRESS
    }

    public static long suggestFee(Priority priority) {
        // TODO: throw error if pre-dymxion is not yet enabled?
        BurstIterator<Block> blocks = Burst.getBlockchain().getBlocks(0, FEE_SUGGESTION_NUMBER_OF_BLOCKS);

        List<List<Long>> fees = extractPaymentFees(blocks);

        int slotCount = Burst.getFluxCapacitor().getInt(FluxInt.MAX_NUMBER_TRANSACTIONS);

        switch (priority) {
            case EXPRESS:
                return suggestFee(fees, FEE_SUGGESTION_EXPRESS_INCLUSION_PROBABILITY, slotCount);
            case NORMAL:
            default:
            return suggestFee(fees, FEE_SUGGESTION_NORMAL_INCLUSION_PROBABILITY, slotCount);
        }
    }

    private static List<List<Long>> extractPaymentFees(BurstIterator<Block> blocks) {
        List<List<Long>> fees = new ArrayList<>();
        for (BurstIterator<Block> it = blocks; it.hasNext(); ) {
            Block block = it.next();
            List<Long> fee = block.getTransactions()
                    .stream()
                    .filter(t -> t.getType().getType() == 0) // TODO: figure type is payment
                    .map(Transaction::getFeeNQT)
                    .collect(Collectors.toList());
            fees.add(fee);
        }
        return fees;
    }

    static long suggestFee(List<List<Long>> historicFees, double inclusionProbability,
                           int slotCount) {
        List<Long> lowestSlotFees = new ArrayList<>(historicFees.size());
        for (List<Long> fees : historicFees) {
            fees.sort(Collections.reverseOrder());

            // assign tx to slot
            boolean[] slots = new boolean[slotCount];
            for (int slot = slotCount, feeIndex = 0; slot >= 0 && feeIndex < fees.size(); --slot) {
                Long fee = fees.get(feeIndex);
                long slotFee = (slot + 1) * FEE_QUANT;
                if (fee >= slotFee) {
                    feeIndex += 1;
                    slots[slot] = true;
                }
            }

            // take min cost slot
            long lowestSlotFee = slotCount * FEE_QUANT;
            for (int i = 0; i < slots.length; ++i) {
                if (!slots[i]) {
                    lowestSlotFee = (i + 1) * FEE_QUANT;
                    break;
                }
            }

            lowestSlotFees.add(lowestSlotFee);
        }

        Collections.sort(lowestSlotFees);

        int index = Math.min(lowestSlotFees.size() - 1, (int) Math.ceil(inclusionProbability * lowestSlotFees.size()));
        return lowestSlotFees.get(index);
    }
}
