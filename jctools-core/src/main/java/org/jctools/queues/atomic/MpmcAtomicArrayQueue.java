/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jctools.queues.atomic;

import org.jctools.util.PortableJvmInfo;
import org.jctools.util.RangeUtil;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * NOTE: This class was automatically generated by org.jctools.queues.atomic.JavaParsingAtomicArrayQueueGenerator
 * which can found in the jctools-build module. The original source file is MpmcArrayQueue.java.
 */
abstract class MpmcAtomicArrayQueueL1Pad<E> extends SequencedAtomicReferenceArrayQueue<E> {

    long p00, p01, p02, p03, p04, p05, p06, p07;

    long p10, p11, p12, p13, p14, p15, p16;

    public MpmcAtomicArrayQueueL1Pad(int capacity) {
        super(capacity);
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.atomic.JavaParsingAtomicArrayQueueGenerator
 * which can found in the jctools-build module. The original source file is MpmcArrayQueue.java.
 */
abstract class MpmcAtomicArrayQueueProducerIndexField<E> extends MpmcAtomicArrayQueueL1Pad<E> {

    private static final AtomicLongFieldUpdater<MpmcAtomicArrayQueueProducerIndexField> P_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater(MpmcAtomicArrayQueueProducerIndexField.class, "producerIndex");

    private volatile long producerIndex;

    public MpmcAtomicArrayQueueProducerIndexField(int capacity) {
        super(capacity);
    }

    @Override
    public final long lvProducerIndex() {
        return producerIndex;
    }

    protected final boolean casProducerIndex(long expect, long newValue) {
        return P_INDEX_UPDATER.compareAndSet(this, expect, newValue);
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.atomic.JavaParsingAtomicArrayQueueGenerator
 * which can found in the jctools-build module. The original source file is MpmcArrayQueue.java.
 */
abstract class MpmcAtomicArrayQueueL2Pad<E> extends MpmcAtomicArrayQueueProducerIndexField<E> {

    long p01, p02, p03, p04, p05, p06, p07;

    long p10, p11, p12, p13, p14, p15, p16, p17;

    public MpmcAtomicArrayQueueL2Pad(int capacity) {
        super(capacity);
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.atomic.JavaParsingAtomicArrayQueueGenerator
 * which can found in the jctools-build module. The original source file is MpmcArrayQueue.java.
 */
abstract class MpmcAtomicArrayQueueConsumerIndexField<E> extends MpmcAtomicArrayQueueL2Pad<E> {

    private static final AtomicLongFieldUpdater<MpmcAtomicArrayQueueConsumerIndexField> C_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater(MpmcAtomicArrayQueueConsumerIndexField.class, "consumerIndex");

    private volatile long consumerIndex;

    public MpmcAtomicArrayQueueConsumerIndexField(int capacity) {
        super(capacity);
    }

    @Override
    public final long lvConsumerIndex() {
        return consumerIndex;
    }

    protected final boolean casConsumerIndex(long expect, long newValue) {
        return C_INDEX_UPDATER.compareAndSet(this, expect, newValue);
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.atomic.JavaParsingAtomicArrayQueueGenerator
 * which can found in the jctools-build module. The original source file is MpmcArrayQueue.java.
 */
abstract class MpmcAtomicArrayQueueL3Pad<E> extends MpmcAtomicArrayQueueConsumerIndexField<E> {

    long p01, p02, p03, p04, p05, p06, p07;

    long p10, p11, p12, p13, p14, p15, p16, p17;

    public MpmcAtomicArrayQueueL3Pad(int capacity) {
        super(capacity);
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.atomic.JavaParsingAtomicArrayQueueGenerator
 * which can found in the jctools-build module. The original source file is MpmcArrayQueue.java.
 
 * A Multi-Producer-Multi-Consumer queue based on a {@link ConcurrentCircularArrayQueue}. This implies that
 * any and all threads may call the offer/poll/peek methods and correctness is maintained. <br>
 * This implementation follows patterns documented on the package level for False Sharing protection.<br>
 * The algorithm for offer/poll is an adaptation of the one put forward by D. Vyukov (See <a
 * href="http://www.1024cores.net/home/lock-free-algorithms/queues/bounded-mpmc-queue">here</a>). The original
 * algorithm uses an array of structs which should offer nice locality properties but is sadly not possible in
 * Java (waiting on Value Types or similar). The alternative explored here utilizes 2 arrays, one for each
 * field of the struct. There is a further alternative in the experimental project which uses iteration phase
 * markers to achieve the same algo and is closer structurally to the original, but sadly does not perform as
 * well as this implementation.<br>
 * <p>
 * Tradeoffs to keep in mind:
 * <ol>
 * <li>Padding for false sharing: counter fields and queue fields are all padded as well as either side of
 * both arrays. We are trading memory to avoid false sharing(active and passive).
 * <li>2 arrays instead of one: The algorithm requires an extra array of longs matching the size of the
 * elements array. This is doubling/tripling the memory allocated for the buffer.
 * <li>Power of 2 capacity: Actual elements buffer (and sequence buffer) is the closest power of 2 larger or
 * equal to the requested capacity.
 * </ol>
 *
 * @param <E> type of the element stored in the {@link java.util.Queue}
 */
public class MpmcAtomicArrayQueue<E> extends MpmcAtomicArrayQueueL3Pad<E> {

    public MpmcAtomicArrayQueue(final int capacity) {
        super(RangeUtil.checkGreaterThanOrEqual(capacity, 2, "capacity"));
    }

    @Override
    public boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException();
        }
        final int mask = this.mask;
        final long capacity = mask + 1;
        final AtomicLongArray sBuffer = sequenceBuffer;
        long pIndex;
        int seqOffset;
        long seq;
        // start with bogus value, hope we don't need it
        long cIndex = Long.MIN_VALUE;
        do {
            pIndex = lvProducerIndex();
            seqOffset = calcSequenceOffset(pIndex, mask);
            seq = lvSequence(sBuffer, seqOffset);
            // consumer has not moved this seq forward, it's as last producer left
            if (seq < pIndex) {
                // Extra check required to ensure [Queue.offer == false iff queue is full]
                if (// test against cached cIndex
                pIndex - capacity >= cIndex && pIndex - capacity >= (cIndex = lvConsumerIndex())) {
                    // test against latest cIndex
                    return false;
                } else {
                    // (+) hack to make it go around again without CAS
                    seq = pIndex + 1;
                }
            }
        } while (// another producer has moved the sequence(or +)
        seq > pIndex || // failed to increment
        !casProducerIndex(pIndex, pIndex + 1));
        soElement(buffer, calcElementOffset(pIndex, mask), e);
        // seq++;
        soSequence(sBuffer, seqOffset, pIndex + 1);
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because return null indicates queue is empty we cannot simply rely on next element visibility for poll
     * and must test producer index when next element is not visible.
     */
    @Override
    public E poll() {
        // local load of field to avoid repeated loads after volatile reads
        final AtomicLongArray sBuffer = sequenceBuffer;
        final int mask = this.mask;
        long cIndex;
        long seq;
        int seqOffset;
        long expectedSeq;
        // start with bogus value, hope we don't need it
        long pIndex = -1;
        do {
            cIndex = lvConsumerIndex();
            seqOffset = calcSequenceOffset(cIndex, mask);
            seq = lvSequence(sBuffer, seqOffset);
            expectedSeq = cIndex + 1;
            if (seq < expectedSeq) {
                // slot has not been moved by producer
                if (// test against cached pIndex
                cIndex >= pIndex && cIndex == (pIndex = lvProducerIndex())) {
                    // strict empty check, this ensures [Queue.poll() == null iff isEmpty()]
                    return null;
                } else {
                    // trip another go around
                    seq = expectedSeq + 1;
                }
            }
        } while (// another consumer beat us to it
        seq > expectedSeq || // failed the CAS
        !casConsumerIndex(cIndex, cIndex + 1));
        final int offset = calcElementOffset(cIndex, mask);
        final E e = lpElement(buffer, offset);
        soElement(buffer, offset, null);
        // i.e. seq += capacity
        soSequence(sBuffer, seqOffset, cIndex + mask + 1);
        return e;
    }

    @Override
    public E peek() {
        long cIndex;
        E e;
        do {
            cIndex = lvConsumerIndex();
            // other consumers may have grabbed the element, or queue might be empty
            e = lpElement(buffer, calcElementOffset(cIndex));
        // only return null if queue is empty
        } while (e == null && cIndex != lvProducerIndex());
        return e;
    }

    @Override
    public boolean relaxedOffer(E e) {
        if (null == e) {
            throw new NullPointerException();
        }
        final int mask = this.mask;
        final AtomicLongArray sBuffer = sequenceBuffer;
        long pIndex;
        int seqOffset;
        long seq;
        do {
            pIndex = lvProducerIndex();
            seqOffset = calcSequenceOffset(pIndex, mask);
            seq = lvSequence(sBuffer, seqOffset);
            if (seq < pIndex) {
                // slot not cleared by consumer yet
                return false;
            }
        } while (// another producer has moved the sequence
        seq > pIndex || // failed to increment
        !casProducerIndex(pIndex, pIndex + 1));
        soElement(buffer, calcElementOffset(pIndex, mask), e);
        soSequence(sBuffer, seqOffset, pIndex + 1);
        return true;
    }

    @Override
    public E relaxedPoll() {
        final AtomicLongArray sBuffer = sequenceBuffer;
        final int mask = this.mask;
        long cIndex;
        int seqOffset;
        long seq;
        long expectedSeq;
        do {
            cIndex = lvConsumerIndex();
            seqOffset = calcSequenceOffset(cIndex, mask);
            seq = lvSequence(sBuffer, seqOffset);
            expectedSeq = cIndex + 1;
            if (seq < expectedSeq) {
                return null;
            }
        } while (// another consumer beat us to it
        seq > expectedSeq || // failed the CAS
        !casConsumerIndex(cIndex, cIndex + 1));
        final int offset = calcElementOffset(cIndex, mask);
        final E e = lpElement(buffer, offset);
        soElement(buffer, offset, null);
        soSequence(sBuffer, seqOffset, cIndex + mask + 1);
        return e;
    }

    @Override
    public E relaxedPeek() {
        long currConsumerIndex = lvConsumerIndex();
        return lpElement(buffer, calcElementOffset(currConsumerIndex));
    }

    @Override
    public int drain(Consumer<E> c) {
        final int capacity = capacity();
        int sum = 0;
        while (sum < capacity) {
            int drained = 0;
            if ((drained = drain(c, PortableJvmInfo.RECOMENDED_POLL_BATCH)) == 0) {
                break;
            }
            sum += drained;
        }
        return sum;
    }

    @Override
    public int fill(Supplier<E> s) {
        // result is a long because we want to have a safepoint check at regular intervals
        long result = 0;
        final int capacity = capacity();
        do {
            final int filled = fill(s, PortableJvmInfo.RECOMENDED_OFFER_BATCH);
            if (filled == 0) {
                return (int) result;
            }
            result += filled;
        } while (result <= capacity);
        return (int) result;
    }

    @Override
    public int drain(Consumer<E> c, int limit) {
        final AtomicLongArray sBuffer = sequenceBuffer;
        final int mask = this.mask;
        final AtomicReferenceArray<E> buffer = this.buffer;
        long cIndex;
        int seqOffset;
        long seq;
        long expectedSeq;
        for (int i = 0; i < limit; i++) {
            do {
                cIndex = lvConsumerIndex();
                seqOffset = calcSequenceOffset(cIndex, mask);
                seq = lvSequence(sBuffer, seqOffset);
                expectedSeq = cIndex + 1;
                if (seq < expectedSeq) {
                    return i;
                }
            } while (// another consumer beat us to it
            seq > expectedSeq || // failed the CAS
            !casConsumerIndex(cIndex, cIndex + 1));
            final int offset = calcElementOffset(cIndex, mask);
            final E e = lpElement(buffer, offset);
            soElement(buffer, offset, null);
            soSequence(sBuffer, seqOffset, cIndex + mask + 1);
            c.accept(e);
        }
        return limit;
    }

    @Override
    public int fill(Supplier<E> s, int limit) {
        final AtomicLongArray sBuffer = sequenceBuffer;
        final int mask = this.mask;
        final AtomicReferenceArray<E> buffer = this.buffer;
        long pIndex;
        int seqOffset;
        long seq;
        for (int i = 0; i < limit; i++) {
            do {
                pIndex = lvProducerIndex();
                seqOffset = calcSequenceOffset(pIndex, mask);
                seq = lvSequence(sBuffer, seqOffset);
                if (seq < pIndex) {
                    // slot not cleared by consumer yet
                    return i;
                }
            } while (// another producer has moved the sequence
            seq > pIndex || // failed to increment
            !casProducerIndex(pIndex, pIndex + 1));
            soElement(buffer, calcElementOffset(pIndex, mask), s.get());
            soSequence(sBuffer, seqOffset, pIndex + 1);
        }
        return limit;
    }

    @Override
    public void drain(Consumer<E> c, WaitStrategy w, ExitCondition exit) {
        int idleCounter = 0;
        while (exit.keepRunning()) {
            if (drain(c, PortableJvmInfo.RECOMENDED_POLL_BATCH) == 0) {
                idleCounter = w.idle(idleCounter);
                continue;
            }
            idleCounter = 0;
        }
    }

    @Override
    public void fill(Supplier<E> s, WaitStrategy w, ExitCondition exit) {
        int idleCounter = 0;
        while (exit.keepRunning()) {
            if (fill(s, PortableJvmInfo.RECOMENDED_OFFER_BATCH) == 0) {
                idleCounter = w.idle(idleCounter);
                continue;
            }
            idleCounter = 0;
        }
    }
}
