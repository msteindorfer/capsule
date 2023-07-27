/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.core;

import static io.usethesource.capsule.core.PersistentTrieVector.VectorNode.BIT_COUNT_OF_INDEX;
import static io.usethesource.capsule.core.PersistentTrieVector.VectorNode.BIT_PARTITION_MASK;
import static io.usethesource.capsule.core.PersistentTrieVector.VectorNode.BIT_PARTITION_SIZE;
import static io.usethesource.capsule.util.ArrayUtils.copyAndDrop;
import static io.usethesource.capsule.util.ArrayUtils.copyAndInsert;
import static io.usethesource.capsule.util.ArrayUtils.copyAndRemove;
import static io.usethesource.capsule.util.ArrayUtils.copyAndSet;
import static io.usethesource.capsule.util.ArrayUtils.copyAndTake;
import static io.usethesource.capsule.util.ArrayUtils.copyAndUpdate;
import static io.usethesource.capsule.util.BitmapUtils.bitpos;
import static io.usethesource.capsule.util.BitmapUtils.index;
import static io.usethesource.capsule.util.BitmapUtils.mask;
import static io.usethesource.capsule.util.FunctionUtils.isInstanceOf;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.usethesource.capsule.Vector;
import io.usethesource.capsule.core.PersistentTrieVector.PathVisitor.Arguments;

public class PersistentTrieVector<K> implements Vector.Immutable<K> {

  private static final VectorNode EMPTY_NODE = VectorNode.of(0, new Object[]{});

  private static final PersistentTrieVector EMPTY_VECTOR =
      new PersistentTrieVector(EMPTY_NODE, 0, 0);

  private final VectorNode<K> root;
  private final int shift;
  private final int length;
  // private final Object[] head;
  // private final Object[] tail;

  public PersistentTrieVector(VectorNode<K> root, int shift, int length) {
    this.root = root;
    this.shift = shift;
    this.length = length;

    assert root.size() == length;
  }

  public static final <K> Vector.Immutable<K> of() {
    return EMPTY_VECTOR;
  }

  public static final <K> Vector.Immutable<K> of(K item) {
    final VectorNode<K> newRootNode = VectorNode.of(0, new Object[]{item});
    return new PersistentTrieVector<>(newRootNode, 0, 1);
  }

  @Override
  public int size() {
    return length;
  }

  @Override
  public Optional<K> get(int index) {
    return root.get(index, index, shift);
  }

  private static final int minimumShift(final int index) {
    int bitWidth = BIT_COUNT_OF_INDEX - Integer.numberOfLeadingZeros(index);

    if (bitWidth % BIT_PARTITION_SIZE == 0) {
      return Math.max(0, (bitWidth / BIT_PARTITION_SIZE) - 1) * BIT_PARTITION_SIZE;
    } else {
      return (bitWidth / BIT_PARTITION_SIZE) * BIT_PARTITION_SIZE;
    }
  }

  // TODO: move to a proper place
  private static final boolean implies(boolean a, boolean b) {
    return !a || b;
  }

  private static final boolean implies(boolean a, BooleanSupplier b) {
    return !a || b.getAsBoolean();
  }

  /*
   * NOTE: the 'left shadow' is always explicit (newRelaxedPath), because by default
   * vectors are left-aligned and right-ragged.
   */
  @Override
  public Vector.Immutable<K> pushFront(K item) {
    final int newShift = root.hasFullFront() ? shift + BIT_PARTITION_SIZE : shift;
    final int newLength = length + 1;

    assert implies(newShift > shift, root.hasFullFront());
    assert implies(newShift == shift, !root.hasFullFront());

    if (newShift > shift) {
      final VectorNode<K> newRootNode = VectorNode.of(newShift, 1, new VectorNode[]{
          newLeftFringedPath(item, shift),
          root
      }, length);

      return new PersistentTrieVector<>(newRootNode, newShift, newLength);
    }

    final VectorNode<K> newRootNode = root.pushFront(shift, item);
    return new PersistentTrieVector<>(newRootNode, shift, newLength);
  }

  /*
   * NOTE: here you can control if the 'right shadow' is implicit (newRegularPath)
   * or explicit at a higher cost (newRelaxedPath).
   */
  @Override
  public Vector.Immutable<K> pushBack(K item) {
    final int newShift = root.hasFullBack() ? shift + BIT_PARTITION_SIZE : shift;
    final int newLength = length + 1;

    assert implies(newShift > shift, root.hasFullBack());
    assert implies(newShift == shift, !root.hasFullBack());

    if (newShift > shift) {
      final VectorNode<K> newRootNode = VectorNode.of(newShift, length, new VectorNode[]{
          root,
          newRightFringedPath(item, shift)
      }, 1);

      return new PersistentTrieVector<>(newRootNode, newShift, newLength);
    }

    final VectorNode<K> newRootNode = root.pushBack(shift, item);
    return new PersistentTrieVector<>(newRootNode, shift, newLength);
  }

  @Override
  public Immutable<K> insertAt(int index, K item) {
    if (index < 0 || index > length) {
      throw new IndexOutOfBoundsException(
          String.format("Index %d out of interval [0,%d]", index, length));
    }

    if (index == 0) {
      return pushFront(item);
    }

    if (index == length) {
      return pushBack(item);
    }

    final Vector.Immutable<K> lhs = take(index);
    final Vector.Immutable<K> rhs = drop(index);

    final Vector.Immutable<K> tmp = lhs.pushBack(item);
    final Vector.Immutable<K> res = tmp.concatenate(rhs);

    return res;
  }

  @Override
  public Vector.Immutable<K> update(int index, K item) {
    if (index < 0 || index >= length) {
      throw new IndexOutOfBoundsException(
          String.format("Index %d out of interval [0,%d)", index, length));
    }

    final VectorNode<K> newRootNode = root.update(index, index, shift, item);
    return new PersistentTrieVector<>(newRootNode, shift, length);
  }

  @Override
  public Vector.Immutable<K> take(int count) {
    if (count <= 0) {
      return EMPTY_VECTOR;
    } else if (count >= size()) {
      return this;
    } else {
      int newShift = shift;
      VectorNode<K> newRootNode = root.take(count - 1, count - 1, shift);

      while (newRootNode.canReduceShift()) {
        newShift -= BIT_PARTITION_SIZE;
        newRootNode = newRootNode.reduceShift();
      }

      return new PersistentTrieVector<>(newRootNode, newShift, count);
    }
  }

  @Override
  public Vector.Immutable<K> drop(int count) {
    if (count <= 0) {
      return this;
    } else if (count >= size()) {
      return EMPTY_VECTOR;
    } else {
      int newShift = shift;
      VectorNode<K> newRootNode = root.drop(count, count, shift);

      while (newRootNode.canReduceShift()) {
        newShift -= BIT_PARTITION_SIZE;
        newRootNode = newRootNode.reduceShift();
      }

      return new PersistentTrieVector<>(newRootNode, newShift, length - count);
    }
  }

  @Override
  public Vector.Immutable<K> concatenate(Vector.Immutable<K> that) {
    if (this.size() == 0) return that;
    if (that.size() == 0) return this;

    if (this.size() == 1) {
      K item = this.get(0).get();
      return that.pushFront(item);
    }

    // TODO: optimize placeholder snippet
    Vector.Immutable<K> tmp = this;

//    // TODO: requires conformance to `java.lang.Iterable`
//    for (K item : that) {
//      tmp = tmp.pushBack(item);
//    }

    for (int i = 0; i < that.size(); i++) {
      tmp = tmp.pushBack(that.get(i).get());
    }

    return tmp;
  }

  private int segmentCount(int length) {
    int fullSegmentCount = length >>> BIT_PARTITION_SIZE;
    int halfSegmentCount = (length & BIT_PARTITION_MASK) != 0 ? 1 : 0;

    int segmentCount = fullSegmentCount + halfSegmentCount;
    return segmentCount;
  }

  static class Path {

    private final int shift;
    private final VectorNode[] nodes;

    Path(int shift) {
      this(shift, new VectorNode[(shift / BIT_PARTITION_SIZE) + 1]);
    }

    Path(int shift, VectorNode[] path) {
      this.shift = shift;
      this.nodes = path;
    }

    Optional<VectorNode> nodeAtShift(int shift) {
      int index = shift / BIT_PARTITION_SIZE;

      if (0 <= index && index < nodes.length) {
        return Optional.of(nodes[index]);
      } else {
        return Optional.empty();
      }
    }

    void put(int shift, VectorNode item) {
      int index = shift / BIT_PARTITION_SIZE;
      nodes[index] = item;
    }

    VectorNode top() {
      return nodes[nodes.length - 1];
    }

//    void pushFront(VectorNode node) {
//
//    }
//
//    void pushBack(VectorNode node) {
//
//    }

  }

  // TODO: simplify
  private static final <K> VectorNode<K> newLeftProlongedPath(int shift, VectorNode<K> node, int shiftAtNode) {
    assert shift >= 0;
    assert shift >= shiftAtNode;

    if (shift == shiftAtNode) {
      return node;
    } else {
      final VectorNode[] dst = new VectorNode[]{
          newLeftProlongedPath(shift - BIT_PARTITION_SIZE, node, shiftAtNode)
      };
      final VectorNode<K> newNode = VectorNode.of(shift, 0, dst, node.size());

      return newNode;
    }
  }

  // TODO: simplify
  private static final <K> VectorNode<K> newRightProlongedPath(int shift, VectorNode<K> node, int shiftAtNode) {
    assert shift >= 0;
    assert shift >= shiftAtNode;

    if (shift == shiftAtNode) {
      return node;
    } else {
      final VectorNode[] dst = new VectorNode[]{
          newRightProlongedPath(shift - BIT_PARTITION_SIZE, node, shiftAtNode)
      };
      final VectorNode<K> newNode = VectorNode.of(shift, node.size(), dst, 0);

      return newNode;
    }
  }

  // TODO: simplify
  private static final <K> VectorNode<K> newLeftFringedPath(K item, int shift) {
    if (shift == 0) {
      return VectorNode.of(0, new Object[]{item});
    } else {
      final VectorNode[] dst = new VectorNode[]{
          newLeftFringedPath(item, shift - BIT_PARTITION_SIZE)
      };
      return VectorNode.of(shift, 1, dst, 0);
    }
  }

  // TODO: simplify
  private static final <K> VectorNode<K> newRightFringedPath(K item, int shift) {
    if (shift == 0) {
      return VectorNode.of(0, new Object[]{item});
    } else {
      final VectorNode[] dst = new VectorNode[]{
          newRightFringedPath(item, shift - BIT_PARTITION_SIZE)
      };
      return VectorNode.of(shift, 0, dst, 1);
    }
  }

  interface NodeVisitor<R, A, E extends Throwable> {

    R visitFringedNode(FringedVectorNode node, A args); // throws E

    R visitLeafNode(ContentVectorNode node, A args); // throws E

  }

  interface VectorNode<K> {

    int BIT_COUNT_OF_INDEX = 32;
    int BIT_PARTITION_SIZE = 5;
    int BIT_PARTITION_MASK = 0b11111;

    /*
     * NOTE: pretty bad performance
     */
    @Deprecated
    int size();

    Optional<K> get(int index, int remainder, int shift);

    boolean hasRegularFront();

    boolean hasRegularBack();

    boolean hasFullFront();

    boolean hasFullBack();

    int sizeFringeL();

    int sizeFringeR();

    boolean canReduceShift();

    VectorNode<K> reduceShift();

    VectorNode<K> pushFront(int shift, K item);

    VectorNode<K> pushBack(int shift, K item);

    VectorNode<K> update(int index, int remainder, int shift, K item);

    // TODO: next up: dropFront() and dropFront(int count)
    // TODO: next up: dropBack () and dropBack (int count)

    // TODO: next up: takeFront(int count)
    // TODO: next up: takeBack (int count)

    /**
     * @param index of the last element we consume
     */
    VectorNode<K> take(int index, int remainder, int shift);

    /**
     * @param index of the first element that remains
     */
    VectorNode<K> drop(int index, int remainder, int shift);

    // TODO: implement in all `VectorNode` sub-types
    default VectorNode<K> first() {
      return null;
    }

    // TODO: implement in all `VectorNode` sub-types
    default VectorNode<K> last() {
      return null;
    }

    // TODO: implement in all `VectorNode` sub-types
    default VectorNode<K> init(int shift) {
      return null;
    }

    // TODO: implement in all `VectorNode` sub-types
    default VectorNode<K> tail(int shift) {
      return null;
    }

    <T, A, E extends Throwable> T accept(NodeVisitor<T, A, E> visitor, A args); // throws E

    static <K> VectorNode<K> of(int shiftWitness, Object[] dst) {
      return new ContentVectorNode<>(dst);
    }

    static <K> VectorNode<K> of(int shiftWitness, int sizeFringeL, VectorNode[] dst,
        int sizeFringeR) {

      assert sizeFringeL <= 1 << shiftWitness;
      assert sizeFringeR <= 1 << shiftWitness;

      final int normalizedFringeL = (sizeFringeL == 1 << shiftWitness) ? 0 : sizeFringeL;
      final int normalizedFringeR = (sizeFringeR == 1 << shiftWitness) ? 0 : sizeFringeR;

//      // TODO: do not support empty nodes mid-tree
//      if (dst.length == 0) {
//        // assert shiftWitness == 0;
//        assert sizeFringeL == 0;
//        assert sizeFringeR == 0;
//
//        return EMPTY_NODE;
//      }

      // return new FringedVectorNode<>(normalizedFringeL, dst, normalizedFringeR);
      return calculateSizes(shiftWitness, dst);
    }

  }

  private final static int[] copyAndSum(int[] src) {
    final int[] dst = new int[src.length];

    int cumulativeSum = 0;
    for (int i = 0; i < src.length; i++) {
      dst[i] = (cumulativeSum += src[i]);
    }
    return dst;
  }

  private final static <K> FringedVectorNode<K> calculateSizes(int shift, VectorNode[] content) {

    final int[] contentSizesSingle =
        IntStream.range(0, content.length).map(i -> content[i].size()).toArray();

    final int[] contentSizesSummed = copyAndSum(contentSizesSingle);

    final IntPredicate isFull = (i) -> contentSizesSingle[i] == 1 << shift;

//    final int[] indices = IntStream.range(0, content.length).filter(isFull.negate()).toArray();
//
//    final int[] compactedSizesSingle =
//        IntStream.of(indices).map(i -> contentSizesSingle[i]).toArray();
//
//    final int[] compactedSizesSummed =
//        IntStream.of(indices).map(i -> contentSizesSummed[i]).toArray();
//
//    final int compactedSizemap =
//        IntStream.of(indices).map(i -> 1 << i).reduce(0, (x, y) -> x | y);

    final int compactedSizemap =
        IntStream.range(0, content.length)
            .filter(isFull.negate())
            .map(i -> 1 << i)
            .reduce(0, (x, y) -> x | y);

    final int l;
    final int r;

    if (content.length == 0) {
      l = 0;
      r = 0;
    } else {
      // NOTE: nodes of size 1 have {@code l == r}; TODO: make invariant?
      l = contentSizesSingle[0];
      r = contentSizesSingle[content.length - 1];

//      l = content[0].sizeFringeL();
//      r = content[content.length - 1].sizeFringeR();
    }

    final int sizeFringeL = (l == 1 << shift) ? 0 : l;
    final int sizeFringeR = (r == 1 << shift) ? 0 : r;

//    if (content.length != 0) {
////      assert (1 << shift) - (1 << shift - BIT_COUNT_OF_INDEX) + sizeFringeL
////          == contentSizesSingle[0] % (1 << shift);
//////          == content[0].sizeFringeL();
////
////      assert (1 << shift) - (1 << shift - BIT_COUNT_OF_INDEX) + sizeFringeR
////          == contentSizesSingle[content.length - 1] % (1 << shift);
//////          == content[content.length - 1].sizeFringeR();
//
//      assert sizeFringeL == content[0].sizeFringeL();
//      assert sizeFringeR == content[content.length - 1].sizeFringeR();
//    }

    // TODO: avoid situations when adjacent nodes underflow (can also happen when using `take` or `drop`
    if (contentSizesSummed.length > 1 && contentSizesSummed[contentSizesSummed.length - 1] == contentSizesSummed.length) {
      // TODO: uncomment for assert stopping for triaging
      // assert false; // all adjacent singleton nodes
    }

    return new FringedVectorNode<K>(sizeFringeL, compactedSizemap, sizeFringeR, contentSizesSingle, contentSizesSummed, content);
  }

  private static final class FringedVectorNode<K> implements VectorNode<K> {

    private static final int[] EMPTY_SIZES = new int[0];

    private final int sizeFringeL;
    private final VectorNode[] content;
    private final int sizeFringeR;

    private int sizemap = 0;
    private int[] sizesSingle = EMPTY_SIZES;
    private int[] sizesSummed = EMPTY_SIZES;

    private FringedVectorNode(int sizeFringeL, VectorNode[] content, int sizeFringeR) {
      assert implies(sizeFringeL == 32, () -> !(content[0] instanceof ContentVectorNode && content[0].size() == 32));

      this.sizeFringeL = sizeFringeL;
      this.content = content;
      this.sizeFringeR = sizeFringeR;

      // TODO implement assertions
      // assert content.length >= 2; // TODO: lazy expansion / path compression
      // assert content.length == 2 && sizeFringeL < sizeFringeR;
      assert implies(content.length == 0, sizeFringeL == 0 && sizeFringeR == 0);
      assert implies(sizeFringeL != 0, content.length > 0 && content[0].size() == sizeFringeL);
      assert implies(sizeFringeR != 0, content.length > 0 && content[content.length - 1].size() == sizeFringeR);
//      assert implies(sizeFringeL != 0, content.length > 0 && content[0].sizeFringeL() == sizeFringeL);
//      assert implies(sizeFringeR != 0, content.length > 0 && content[content.length - 1].sizeFringeR() == sizeFringeR);
    }

    private FringedVectorNode(int l, int b, int r, int[] sizesSingle, int[] sizesSummed, VectorNode[] content) {
      this(l, content, r);

      this.sizemap = b;
      this.sizesSingle = sizesSingle;
      this.sizesSummed = sizesSummed;

      // TODO: define invariants with asserts
      assert size() == Stream.of(content).mapToInt(VectorNode::size).sum();
    }

    /*
     * TODO: improve performance (binary search, etc)
     */
    private final static int offset(int[] cumulativeSizes, int index) {
      for (int i = 0; i < cumulativeSizes.length; i++) {
        if (cumulativeSizes[i] > index) {
          return i;
        }
      }
      throw new IndexOutOfBoundsException("Index larger than subtree.");
    }

    private int lazySize = 0;

    @Override
    public int size() {
      if (lazySize == 0) {
//        final int size;
//
//        if (sizemap == 0) {
//          size = ???;
//        } else {
//          size = Stream.of(content).mapToInt(VectorNode::size).sum();
//        }
//
//        lazySize = size;

        if (content.length == 0) {
          lazySize = 0;
        } else {
          lazySize = sizesSummed[content.length - 1];
        }

//        lazySize = Stream.of(content).mapToInt(VectorNode::size).sum();
      }

      return lazySize;
    }

    private final boolean isFullRegular() {
      final boolean isFullRegular;

      if (content.length < 2) {
        // first and last node equal
        isFullRegular = 0 == sizemap;
      } else {
        // ignore size of last node
        isFullRegular = 0 == (sizemap & ~((1 << (content.length - 1))));
      }

      assert implies(isFullRegular, sizeFringeL == 0);
      return isFullRegular;
    }

    private final boolean isSemiRegular() {
      final boolean isSemiRegular;

      if (content.length < 2) {
        // first and last node equal
        isSemiRegular = 1 == sizemap;
      } else {
        // ignore size of last node
        isSemiRegular = 1 == (sizemap & ~((1 << (content.length - 1))));
      }
      assert implies(isSemiRegular, sizeFringeL != 0);

      return isSemiRegular;
    }

    private final boolean isEffectivelyRegular(int remainder, int shift) {
      final int __mask = mask(remainder, shift, BIT_PARTITION_MASK);
      final int __index = index(sizemap, __mask, bitpos(__mask));
      return (sizemap & bitpos(__mask)) == 0 && __index == 0;
    }

    private Object[] nextNodeAndArguments(PathVisitor.Arguments args) {
      assert 0 <= args.index;
      assert 0 <= args.remainder;

      final int blockRelativeIndex;
      final int newRemainder;

      if (isFullRegular() || isEffectivelyRegular(args.remainder, args.shift)
          || args.remainder < sizeFringeL) {
        // regular (or in first sub-tree)
        blockRelativeIndex = mask(args.remainder, args.shift, BIT_PARTITION_MASK);
        newRemainder = args.remainder & ~(BIT_PARTITION_MASK << args.shift);
      } else if (isSemiRegular()) {
        // semi-regular
        // TODO: support {@code isEffectivelyRegular} in semi-regular
        blockRelativeIndex = 1 + ((args.remainder - sizeFringeL) >>> args.shift);
        newRemainder = (args.remainder - sizeFringeL) & ~(BIT_PARTITION_MASK << args.shift);
      } else {
        // irregular
        blockRelativeIndex = offset(sizesSummed, args.remainder);
        newRemainder = (blockRelativeIndex == 0)
            ? args.remainder
            : args.remainder - sizesSummed[blockRelativeIndex - 1];
      }

      final VectorNode<K> nextNode = content[blockRelativeIndex];

      final PathVisitor.Arguments nextArgs =
          PathVisitor.Arguments.of(args.index, newRemainder, args.shift - BIT_PARTITION_SIZE);

      return new Object[]{blockRelativeIndex, nextNode, nextArgs};
    }

    private int nextNodeIndex(PathVisitor.Arguments args) {
      return (Integer) nextNodeAndArguments(args)[0];
    }

    private VectorNode<K> nextNode(PathVisitor.Arguments args) {
      return (VectorNode<K>) nextNodeAndArguments(args)[1];
    }

    private PathVisitor.Arguments nextArguments(PathVisitor.Arguments args) {
      return (PathVisitor.Arguments) nextNodeAndArguments(args)[2];
    }

    /*
     * TODO: unify with {@code #update}, only recursive function call differs.
     */
    @Override
    public Optional<K> get(int index, int remainder, int shift) {
      final PathVisitor.Arguments args = Arguments.of(index, remainder, shift);

      final VectorNode<K> nextNode = this.nextNode(args);
      final PathVisitor.Arguments nextArgs = this.nextArguments(args);

      return nextNode.get(nextArgs.index, nextArgs.remainder, nextArgs.shift);
    }

    @Override
    public boolean hasRegularFront() {
      return sizeFringeL == 0;
    }

    @Override
    public boolean hasRegularBack() {
      return sizeFringeR == 0;
    }

    public boolean hasFullFront() {
      return hasRegularFront() && content.length == BIT_COUNT_OF_INDEX;
    }

    public boolean hasFullBack() {
      return hasRegularBack() && content.length == BIT_COUNT_OF_INDEX;
    }

    @Override
    public int sizeFringeL() {
      return sizeFringeL;
    }

    @Override
    public int sizeFringeR() {
      return sizeFringeR;
    }

    @Override
    public boolean canReduceShift() {
      return content.length == 1;
    }

    @Override
    public VectorNode<K> reduceShift() {
      if (canReduceShift()) {
        return content[0];
      } else {
        return this; // no-op
      }
    }

    @Override
    public VectorNode<K> pushFront(int shift, K item) {
      boolean isSubTreeBranchFull = sizeFringeL == 0;
      boolean isCurrentBranchFull = isSubTreeBranchFull && content.length == BIT_COUNT_OF_INDEX;

      if (!isSubTreeBranchFull) {
        final VectorNode[] dst = copyAndUpdate(VectorNode[]::new, content, 0,
            node -> node.pushFront(shift - BIT_PARTITION_SIZE, item));

        return VectorNode.of(shift, sizeFringeL + 1, dst, sizeFringeR);
      }

      if (!isCurrentBranchFull) {
        final VectorNode[] dst = copyAndInsert(VectorNode[]::new, content, 0,
            newLeftFringedPath(item, shift - BIT_PARTITION_SIZE));

        return VectorNode.of(shift, 1, dst, sizeFringeR);
      }

      throw new IllegalStateException("Prepending not fully implemented.");
    }

    @Override
    public VectorNode<K> pushBack(int shift, K item) {
      boolean isSubTreeBranchFull = sizeFringeR == 0;
      boolean isCurrentBranchFull = isSubTreeBranchFull && content.length == BIT_COUNT_OF_INDEX;

      if (!isSubTreeBranchFull) {
        final VectorNode[] dst = copyAndUpdate(VectorNode[]::new, content, content.length - 1,
            node -> node.pushBack(shift - BIT_PARTITION_SIZE, item));

        return VectorNode.of(shift, sizeFringeL, dst, sizeFringeR + 1);
      }

      if (!isCurrentBranchFull) {
        final VectorNode[] dst = copyAndInsert(VectorNode[]::new, content, content.length,
            newRightFringedPath(item, shift - BIT_PARTITION_SIZE));

        return VectorNode.of(shift, sizeFringeL, dst, 1);
      }

      throw new IllegalStateException("Appending not fully implemented.");
    }

    /*
     * TODO: unify with {@code #get}, only recursive function call differs.
     */
    @Override
    public VectorNode<K> update(int index, int remainder, int shift, K item) {
      final PathVisitor.Arguments args = Arguments.of(index, remainder, shift);

      final int blockRelativeIndex = this.nextNodeIndex(args);
      // final VectorNode<K> nextNode = this.nextNode(args);
      final PathVisitor.Arguments nextArgs = this.nextArguments(args);

      VectorNode<K>[] newContent = copyAndUpdate(VectorNode[]::new, content, blockRelativeIndex,
          node -> node.update(nextArgs.index, nextArgs.remainder, nextArgs.shift, item));

      return VectorNode.of(shift, sizeFringeL, newContent, sizeFringeR);
    }

    @Override
    public VectorNode<K> take(int index, int remainder, int shift) {
      final PathVisitor.Arguments args = Arguments.of(index, remainder, shift);

      final int blockRelativeIndex = this.nextNodeIndex(args);
      // final VectorNode<K> nextNode = this.nextNode(args);
      final PathVisitor.Arguments nextArgs = this.nextArguments(args);

      final VectorNode[] dst = copyAndTake(VectorNode[]::new, content, blockRelativeIndex,
          node -> node.take(nextArgs.index, nextArgs.remainder, nextArgs.shift));

      final int newSizeFringeL;
      final int newSizeFringeR;

      if (blockRelativeIndex == 0) {
        // first
        // TODO: how to align first blocks to left or right?
        newSizeFringeL = nextArgs.remainder + 1;
        newSizeFringeR = 0;
      } else if (blockRelativeIndex < content.length - 1) {
        // middle
        newSizeFringeL = sizeFringeL;
        newSizeFringeR = nextArgs.remainder + 1;
      } else {
        // last
        newSizeFringeL = sizeFringeL;
        newSizeFringeR = nextArgs.remainder + 1;
      }

      return VectorNode.of(shift, newSizeFringeL, dst, newSizeFringeR);

    }

    @Override
    public VectorNode<K> drop(int index, int remainder, int shift) {
      final PathVisitor.Arguments args = Arguments.of(index, remainder, shift);

      final int blockRelativeIndex = this.nextNodeIndex(args);
      // final VectorNode<K> nextNode = this.nextNode(args);
      final PathVisitor.Arguments nextArgs = this.nextArguments(args);

      final VectorNode[] dst = copyAndDrop(VectorNode[]::new, content, blockRelativeIndex,
          node -> node.drop(nextArgs.index, nextArgs.remainder, nextArgs.shift));

      final int newSizeFringeL;
      final int newSizeFringeR;

      if (blockRelativeIndex == 0) {
        // first
        // TODO: how to align first blocks to left or right?
        newSizeFringeL = (sizeFringeL == 0)
            ? (1 << shift) - nextArgs.remainder
            : sizeFringeL - nextArgs.remainder;
        newSizeFringeR = sizeFringeR;
      } else if (blockRelativeIndex < content.length - 1) {
        // middle
        newSizeFringeL = (1 << shift) - nextArgs.remainder;
        newSizeFringeR = sizeFringeR;
      } else {
        // last
        newSizeFringeL = 0;
        newSizeFringeR = (sizeFringeR == 0)
            ? (1 << shift) - nextArgs.remainder
            : sizeFringeR - nextArgs.remainder;
      }

      return VectorNode.of(shift, newSizeFringeL, dst, newSizeFringeR);
    }

    @Override
    public VectorNode<K> first() {
      return content[0];
    }

    @Override
    public VectorNode<K> last() {
      return content[content.length - 1];
    }

    @Override
    public VectorNode<K> init(int shift) {
      final VectorNode[] dst = copyAndRemove(VectorNode[]::new, content, content.length - 1);

      return VectorNode.of(shift, sizeFringeL, dst, 0);
    }

    @Override
    public VectorNode<K> tail(int shift) {
      final VectorNode[] dst = copyAndRemove(VectorNode[]::new, content, 0);

      return VectorNode.of(shift, 0, dst, sizeFringeR);
    }

    @Override
    public <T, A, E extends Throwable> T accept(NodeVisitor<T, A, E> visitor, A args) { // throws E
      return visitor.visitFringedNode(this, args);
    }

  }

  private static final class ContentVectorNode<K> implements VectorNode<K> {

    private final Object[] content;

    private ContentVectorNode(Object[] content) {
      this.content = content;

      assert Arrays.stream(content).noneMatch(isInstanceOf(VectorNode.class));
      assert Arrays.stream(content).noneMatch(isInstanceOf(ContentVectorNode.class));
      assert Arrays.stream(content).noneMatch(isInstanceOf(FringedVectorNode.class));
      assert content.length <= BIT_COUNT_OF_INDEX;
    }

    @Override
    public int size() {
      return content.length;
    }

    @Override
    public Optional<K> get(int index, int remainder, int shift) {
      assert shift == 0;
      assert remainder < content.length;

      int blockRelativeIndex = remainder;

      if (blockRelativeIndex >= content.length) {
        return Optional.empty();
      } else {
        return Optional.of((K) content[blockRelativeIndex]);
      }
    }

    public boolean hasRegularFront() {
      return size() == BIT_COUNT_OF_INDEX;
    }

    public boolean hasRegularBack() {
      return size() == BIT_COUNT_OF_INDEX;
    }

    public boolean hasFullFront() {
      return size() == BIT_COUNT_OF_INDEX;
    }

    public boolean hasFullBack() {
      return size() == BIT_COUNT_OF_INDEX;
    }

    @Override
    public int sizeFringeL() {
      return size() % BIT_COUNT_OF_INDEX;
    }

    @Override
    public int sizeFringeR() {
      return size() % BIT_COUNT_OF_INDEX;
    }

    @Override
    public boolean canReduceShift() {
      return false;
    }

    @Override
    public VectorNode<K> reduceShift() {
      return this; // no-op
    }

    @Override
    public VectorNode<K> pushFront(int shift, K item) {
      assert shift == 0;

      final Object[] src = this.content;
      final Object[] dst = copyAndInsert(Object[]::new, src, 0, item);

      // TODO: correct?
      return VectorNode.of(0, dst);
    }

    @Override
    public VectorNode<K> pushBack(int shift, K item) {
      assert shift == 0;

      final Object[] src = this.content;
      final Object[] dst = copyAndInsert(Object[]::new, src, src.length, item);

      // TODO: correct?
      return VectorNode.of(0, dst);
    }

    @Override
    public VectorNode<K> update(int index, int remainder, int shift, K item) {
      assert shift == 0;

      final Object[] src = this.content;
      final Object[] dst = copyAndSet(Object[]::new, src, remainder, item);

      return VectorNode.of(0, dst);
    }

    @Override
    public VectorNode<K> take(int index, int remainder, int shift) {
      assert shift == 0;

      final Object[] src = this.content;
      final Object[] dst = copyAndTake(Object[]::new, src, remainder, item -> item);

      return VectorNode.of(0, dst);
    }

    @Override
    public VectorNode<K> drop(int index, int remainder, int shift) {
      assert shift == 0;

      final Object[] src = this.content;
      final Object[] dst = copyAndDrop(Object[]::new, src, remainder, item -> item);

      return VectorNode.of(0, dst);
    }

    @Override
    public <T, A, E extends Throwable> T accept(NodeVisitor<T, A, E> visitor, A args) { // throws E
      return visitor.visitLeafNode(this, args);
    }

  }

  static class PathVisitor implements NodeVisitor<Path, PathVisitor.Arguments, Throwable> {

    private final Path path;

    PathVisitor(Supplier<Path> path) {
      this.path = path.get();
    }

    static class Arguments {

      final int index;
      final int remainder;
      final int shift;

      Arguments(int index, int remainder, int shift) {
        this.index = index;
        this.remainder = remainder;
        this.shift = shift;

        assert 0 <= index;
        assert 0 <= remainder;
        assert 0 <= shift;
      }

      static Arguments of(int index, int remainder, int shift) {
        return new Arguments(index, remainder, shift);
      }

    }

    @Override
    public Path visitFringedNode(FringedVectorNode node, PathVisitor.Arguments args) {
      path.put(args.shift, node); // mutable update

      final VectorNode nextNode = node.nextNode(args);
      final PathVisitor.Arguments nextArgs = node.nextArguments(args);

      return (Path) nextNode.accept(this, nextArgs);
    }

    @Override
    public Path visitLeafNode(ContentVectorNode node, PathVisitor.Arguments args) {
      path.put(args.shift, node); // mutable update

      return path;
    }

  }

  @Override
  public java.util.Iterator<K> iterator() {
    var indices = java.util.stream.IntStream.range(0, size());

    return indices
            .mapToObj(index -> this.get(index).get())
            .iterator();
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof PersistentTrieVector) {
      final PersistentTrieVector<K> that = (PersistentTrieVector<K>) other;

      if (length != that.length) {
        return false;
      }

      for (int i = 0; i < length; i++) {
        if (!get(i).equals(that.get(i))) {
          return false;
        }
      }

      return true;
    } else {
      throw new UnsupportedOperationException("Not yet implemented.");
    }
  }

//  @Override
//  public int hashCode() {
//    throw new UnsupportedOperationException("Not yet implemented.");
//  }

  @Override
  public String toString() {
    if (length == 0) {
      return "[]";
    }

    StringBuffer sb = new StringBuffer();
    sb.append("[");

    sb.append(get(0).get());
    for (int i = 1; i < length; i++) {
      sb.append(", ");
      sb.append(get(i).get());
    }

    sb.append("]");
    return sb.toString();
  }
}
