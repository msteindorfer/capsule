/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.core;

import static io.usethesource.capsule.core.PersistentTrieVector.VectorNode.BIT_COUNT_OF_INDEX;
import static io.usethesource.capsule.core.PersistentTrieVector.VectorNode.BIT_PARTITION_SIZE;

import java.util.Optional;

import io.usethesource.capsule.Vector;

@SuppressWarnings({"unchecked", "rawtypes"})
public class PersistentTrieVector<K> implements Vector.Immutable<K>, java.util.List<K> {

  private static final VectorNode EMPTY_NODE = new ContentVectorNode<>(new Object[]{});

  private static final PersistentTrieVector EMPTY_VECTOR =
          new PersistentTrieVector(EMPTY_NODE, 0, 0);

  private final VectorNode<K> root;
  private final int shift;
  private final int length;
  // private final Object[] head;
  // private final Object[] tail;

  PersistentTrieVector(VectorNode<K> root, int shift, int length) {
    this.root = root;
    this.shift = shift;
    this.length = length;
  }

  public static <K> Vector.Immutable<K> of() {
    return EMPTY_VECTOR;
  }

  public static <K> Vector.Immutable<K> of(K item) {
    final VectorNode<K> newRootNode = new ContentVectorNode<>(new Object[]{item});
    return new PersistentTrieVector<>(newRootNode, 0, 1);
  }

  @Override
  public int size() {
    return length;
  }

  @Override
  public boolean isEmpty() {
    return length == 0;
  }

  @Override
  public boolean contains(Object object) {
    java.util.function.Predicate<K> predicate = (K element) -> java.util.Objects.equals(element, object);
    return stream().anyMatch(predicate);
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public K get(int index) {
    return root.get(index, shift).get();
  }

  @Override
  public K set(int index, K element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, K element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public K remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(Object object) {
    java.util.function.IntPredicate predicate = (int index) -> java.util.Objects.equals(this.get(index), object);

    return java.util.stream.IntStream.range(0, length)
            .filter(predicate)
            .findFirst()
            .orElse(-1);
  }

  @Override
  public int lastIndexOf(Object object) {
    java.util.function.IntPredicate predicate = (int index) -> java.util.Objects.equals(this.get(index), object);

    return java.util.stream.IntStream.range(0, length)
            .map(index -> length - index - 1) // reverse indices
            .filter(predicate)
            .findFirst()
            .orElse(-1);
  }

  @Override
  public java.util.ListIterator<K> listIterator() {
    throw new UnsupportedOperationException("Not yet implemented."); // TODO: implement to fulfill conformance
  }

  @Override
  public java.util.ListIterator<K> listIterator(int index) {
    throw new UnsupportedOperationException("Not yet implemented."); // TODO: implement to fulfill conformance
  }

  @Override
  public java.util.List<K> subList(int fromIndex, int toIndex) {
    if (fromIndex < 0 || toIndex > length || fromIndex > toIndex) {
      throw new IndexOutOfBoundsException(
              String.format("Sub-list interval [%d,%d) not included in list interval [0,%d)", fromIndex, toIndex, length));
    }

    return stream()
            .skip(fromIndex)
            .limit(toIndex - fromIndex)
            .collect(java.util.stream.Collectors.toUnmodifiableList());
  }

  private static int blockOffset(final int index) {
    if (index < BIT_COUNT_OF_INDEX) {
      return 0;
    } else {
      return ((index - 1) >>> BIT_PARTITION_SIZE) << BIT_PARTITION_SIZE;
    }
  }

  @SuppressWarnings("unused")
  private static int blockRelativeIndex(final int index) {
    return index - blockOffset(index);
  }

  private static int minimumShift(final int index) {
    int bitWidth = BIT_COUNT_OF_INDEX - Integer.numberOfLeadingZeros(index);

    if (bitWidth % BIT_PARTITION_SIZE == 0) {
      return Math.max(0, (bitWidth / BIT_PARTITION_SIZE) - 1) * BIT_PARTITION_SIZE;
    } else {
      return (bitWidth / BIT_PARTITION_SIZE) * BIT_PARTITION_SIZE;
    }
  }

  @Override
  public Vector.Immutable<K> insertAt(int index, K item) {
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

    return lhs.pushBack(item).concatenate(rhs);
  }

  @Override
  public Vector.Immutable<K> update(int index, K item) {
    if (index < 0 || index >= length) {
      throw new IndexOutOfBoundsException(
              String.format("Index %d out of interval [0,%d)", index, length));
    }

    final Vector.Immutable<K> lhs = take(index);
    final Vector.Immutable<K> rhs = drop(index + 1);

    return lhs.pushBack(item).concatenate(rhs);
  }

  @Override
  public Immutable<K> delete(int index) {
    if (index < 0 || index >= length) {
      throw new IndexOutOfBoundsException(
          String.format("Index %d out of interval [0,%d)", index, length));
    }

    final Vector.Immutable<K> lhs = take(index);
    final Vector.Immutable<K> rhs = drop(index + 1);

    return lhs.concatenate(rhs);
  }

  @Override
  public Vector.Immutable<K> take(int count) {
    if (count <= 0) {
      return EMPTY_VECTOR;
    } else if (count >= size()) {
      return this;
    } else {
      // TODO: optimize placeholder snippet
      Vector.Immutable<K> tmp = Vector.Immutable.of();

      java.util.Iterator<K> remainingElements = stream().limit(count).iterator();
      while (remainingElements.hasNext()) {
        tmp = tmp.pushBack(remainingElements.next());
      }

      return tmp;
    }
  }

  @Override
  public Vector.Immutable<K> drop(int count) {
    if (count <= 0) {
      return this;
    } else if (count >= size()) {
      return EMPTY_VECTOR;
    } else {
      // TODO: optimize placeholder snippet
      Vector.Immutable<K> tmp = Vector.Immutable.of();

      java.util.Iterator<K> remainingElements = stream().skip(count).iterator();
      while (remainingElements.hasNext()) {
        tmp = tmp.pushBack(remainingElements.next());
      }

      return tmp;
    }
  }

  @Override
  public Vector.Immutable<K> concatenate(Vector.Immutable<K> that) {
    if (this.size() == 0) return that;
    if (that.size() == 0) return this;

    if (this.size() == 1) {
      K item = this.get(0);
      return that.pushFront(item);
    }

    // TODO: optimize placeholder snippet
    Vector.Immutable<K> tmp = this;

    for (K item : that) {
      tmp = tmp.pushBack(item);
    }

    return tmp;
  }

  public Vector.Immutable<K> pushFront(K item) {
    // TODO: optimize placeholder snippet
    Vector.Immutable<K> tmp = Vector.Immutable.of(item);

    for (K _item : this) {
      tmp = tmp.pushBack(_item);
    }

    return tmp;
  }

  @Override
  public Vector.Immutable<K> pushBack(K item) {
    final int newLength = length + 1;
    final int newShift = minimumShift(length);

    if (newShift > shift) {
      final VectorNode<K> newLeafNode = new ContentVectorNode<>(new Object[]{item});
      final VectorNode<K> newRootNode = new RegularVectorNode<>(new VectorNode[]{
              root,
              newPath(newLeafNode, shift)
      });

      return new PersistentTrieVector<>(newRootNode, newShift, newLength);
    }

    final VectorNode<K> newRootNode = root.pushBack(length, item, shift);
    return new PersistentTrieVector<>(newRootNode, shift, newLength);
  }

  private static <K> VectorNode<K> newPath(VectorNode<K> node, int level) {
    if (level == 0) {
      return node;
    } else {
      final VectorNode[] content = new VectorNode[]{
              newPath(node, level - BIT_PARTITION_SIZE)
      };
      return new RegularVectorNode<>(content);
    }
  }

  @Override
  public boolean isTransientSupported() {
    return true;
  }

  @Override
  public Transient<K> asTransient() {
    return new TransientTrieVector<>(this);
  }

  interface VectorNode<K> {

    int BIT_COUNT_OF_INDEX = 32;
    int BIT_PARTITION_SIZE = 5;
    @SuppressWarnings("unused")
    int BIT_PARTITION_MASK = 0b11111;

    Optional<K> get(int index, int shift);

    VectorNode<K> pushBack(int index, K item, int shift);

  }

  private static final class RegularVectorNode<K> implements VectorNode<K> {

    private final VectorNode[] content;

    private RegularVectorNode(VectorNode[] content) {
      this.content = content;
    }

    @Override
    public Optional<K> get(int index, int shift) {
      int blockRelativeIndex = (index >>> shift) & 0b11111;
      return content[blockRelativeIndex].get(index, shift - BIT_PARTITION_SIZE);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable", "IfStatementWithIdenticalBranches"})
    @Override
    public VectorNode<K> pushBack(int index, K item, int shift) {
      int blockRelativeIndex = (index >>> shift) & 0b11111;

      // assert blockRelativeIndex < content.length;
      assert content.length <= BIT_COUNT_OF_INDEX;

      if (blockRelativeIndex == content.length) {
        // copy and insert node
        final VectorNode[] src = this.content;
        final VectorNode[] dst = new VectorNode[src.length + 1];

        final int idx = blockRelativeIndex;
        final VectorNode<K> newLeafNode = new ContentVectorNode<>(new Object[]{item});
        final VectorNode<K> newNode = newPath(newLeafNode, shift - BIT_PARTITION_SIZE);

        // copy 'src' and insert 1 element(s) at position 'idx'
        System.arraycopy(src, 0, dst, 0, idx);
        dst[idx] = newNode;
        System.arraycopy(src, idx, dst, idx + 1, src.length - idx);

        return new RegularVectorNode<>(dst);
      } else {
        // copy and set node
        final VectorNode[] src = this.content;
        final VectorNode[] dst = new VectorNode[src.length];

        final int idx = blockRelativeIndex;
        final VectorNode<K> newNode = src[idx].pushBack(index, item, shift - BIT_PARTITION_SIZE);

        // copy 'src' and set 1 element(s) at position 'idx'
        System.arraycopy(src, 0, dst, 0, src.length);
        dst[idx] = newNode;

        return new RegularVectorNode<>(dst);
      }
    }

  }

  private static final class ContentVectorNode<K> implements VectorNode<K> {

    private final Object[] content;

    private ContentVectorNode(Object[] content) {
      this.content = content;
    }

    @Override
    public Optional<K> get(int index, int shift) {
      assert shift == 0;
      assert ((index >>> shift) & 0b11111) <= content.length;

      int blockRelativeIndex = (index >>> shift) & 0b11111;

      if (blockRelativeIndex >= content.length) {
        return Optional.empty();
      } else {
        return Optional.of((K) content[blockRelativeIndex]);
      }
    }

    @Override
    public VectorNode<K> pushBack(int index, K item, int shift) {
      assert shift == 0;
      assert content.length < BIT_COUNT_OF_INDEX;

      final Object[] src = this.content;
      final Object[] dst = new Object[src.length + 1];

      final int idx = src.length;

      // copy 'src' and insert 1 element(s) at position 'idx'
      System.arraycopy(src, 0, dst, 0, idx);
      dst[idx] = item;
      System.arraycopy(src, idx, dst, idx + 1, src.length - idx);

      return new ContentVectorNode<>(dst);
    }

  }

  @Override
  public java.util.Iterator<K> iterator() {
    var indices = java.util.stream.IntStream.range(0, size());

    return indices
            .mapToObj(this::get)
            .iterator();
  }

  @SuppressWarnings("SimplifyStreamApiCallChains")
  @Override
  public Object[] toArray() {
    return stream().toArray();
  }

  @SuppressWarnings({"NullableProblems", "SimplifyStreamApiCallChains"})
  @Override
  public <T> T[] toArray(final T[] a) {
    return stream()
            .collect(java.util.stream.Collectors.toList())
            .toArray(a);
  }

  @Override
  public boolean add(K k) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(java.util.Collection<?> collection) {
    return collection.stream().allMatch(this::contains);
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public boolean addAll(java.util.Collection<? extends K> collection) {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public boolean addAll(int index, java.util.Collection<? extends K> collection) {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public boolean removeAll(java.util.Collection<?> collection) {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public boolean retainAll(java.util.Collection<?> collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public java.util.stream.Stream<K> stream() {
    return java.util.stream.StreamSupport.stream(this.spliterator(), false);
  }

  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    }
    if (other == null) {
      return false;
    }

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
    } else if (other instanceof java.util.List) {
      java.util.List that = (java.util.List) other;

      if (this.size() != that.size()) {
        return false;
      }

      for (int i = 0; i < length; i++) {
        if (!get(i).equals(that.get(i))) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  // `hashCode` implementation according to `java.util.List#hashCode()` specification.
  @Override
  public int hashCode() {
    var hashCode = 1;

    for (K item : this) {
      hashCode *= 31;
      hashCode += java.util.Objects.hashCode(item);
    }

    return hashCode;
  }

  @Override
  public String toString() {
    return stream()
            .map(K::toString)
            .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
  }

  static final class TransientTrieVector<K, V> implements
          io.usethesource.capsule.Vector.Transient<K> {

    private Vector.Immutable<K> delegate;

    TransientTrieVector(PersistentTrieVector<K> trieVector) {
      this.delegate = trieVector;
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public boolean isEmpty() {
      return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object object) {
      return delegate.contains(object);
    }

    @Override
    public K get(int index) {
      return delegate.get(index);
    }

    @Override
    public int indexOf(Object object) {
      return delegate.indexOf(object);
    }

    @Override
    public int lastIndexOf(Object object) {
      return delegate.lastIndexOf(object);
    }

    @Override
    public java.util.Iterator<K> iterator() {
      return delegate.iterator();
    }

    @Override
    public boolean insertAt(int index, K item) {
      delegate = delegate.insertAt(index, item);
      return true;
    }

    @Override
    public boolean update(int index, K item) {
      delegate = delegate.update(index, item);
      return true;
    }

    @Override
    public boolean delete(int index) {
      delegate = delegate.delete(index);
      return true;
    }

    @Override
    public boolean pushFront(K item) {
      delegate = delegate.pushFront(item);
      return true;
    }

    @Override
    public boolean pushBack(K item) {
      delegate = delegate.pushBack(item);
      return true;
    }

    @Override
    public Vector.Immutable<K> freeze() {
      return delegate;
    }

  }

}
