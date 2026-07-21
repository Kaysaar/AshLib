package ashlib.shmo.api.general;

import de.unkrig.commons.nullanalysis.Nullable;

import java.util.Objects;

public final class Option<T> {
    @FunctionalInterface
    public interface MatchSomeFunction<T, U> {
        U execute(T value);
    }

    @FunctionalInterface
    public interface MatchNoneFunction<T> {
        T execute();
    }

    @FunctionalInterface
    public interface MatchSomeAction<T> {
        void execute(T value);
    }

    @FunctionalInterface
    public interface MatchNoneAction {
        void execute();
    }

    @FunctionalInterface
    public interface MapFunction<T, U> {
        U execute(T value);
    }

    @FunctionalInterface
    public interface MapAction<T> {
        T execute();
    }

    @FunctionalInterface
    public interface FlatMapFunction<T, U> {
        Option<U> execute(T value);
    }

    @FunctionalInterface
    public interface FlatMapAction<T> {
        Option<T> execute();
    }

    @FunctionalInterface
    public interface ReduceFunction<T, U> {
        U execute(T value);
    }

    @FunctionalInterface
    public interface ReduceDefaultValue<T> {
        T execute();
    }

    @Nullable private final T value;

    public Option() {
        value = null;
    }

    public Option(@Nullable T value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (!(obj instanceof Option<?> other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        if (isNone()) { return "Option( None )"; }
        return "Option( " + value + " )";
    }

    public static <T> Option<T> of(T value) {
        return new Option<>(value);
    }

    public static <T> Option<T> none() {
        return new Option<>(null);
    }

    public boolean isSome() {
        return value != null;
    }

    public boolean isNone() {
        return value == null;
    }

    public T unwrap() throws NullPointerException {
        if (isNone()) { throw new NullPointerException("Tried to unwrap a 'none' Option"); }
        return value;
    }

    public <U> U match(MatchSomeFunction<T, U> onSome, MatchNoneFunction<U> onNone) {
        if (isSome()) { return onSome.execute(value); }
        else { return onNone.execute(); }
    }

    public void match(MatchSomeAction<T> onSome, MatchNoneAction onNone) {
        if (isSome()) { onSome.execute(value); }
        else { onNone.execute(); }
    }

    public <U> Option<U> map(MapFunction<T, U> mapFunction) {
        if (isSome()) {
            return Option.of(mapFunction.execute(value));
        } else {
            return Option.none();
        }
    }

    public <U> Option<U> map(MapAction<U> mapAction) {
        if (isSome()) {
            return Option.of(mapAction.execute());
        } else {
            return Option.none();
        }
    }

    public <U> Option<U> flatMap(FlatMapFunction<T, U> mapFunction) {
        if (isSome()) {
            return mapFunction.execute(value);
        } else {
            return Option.none();
        }
    }

    public <U> Option<U> flatMap(FlatMapAction<U> mapAction) {
        if (isSome()) {
            return mapAction.execute();
        } else {
            return Option.none();
        }
    }

    public <U> U reduce(ReduceFunction<T, U> reduceFunction, U defaultValue) {
        if (isSome()) {
            return reduceFunction.execute(value);
        } else {
            return defaultValue;
        }
    }

    public <U> U reduce(ReduceFunction<T, U> reduceFunction, ReduceDefaultValue<U> defaultValue) {
        if (isSome()) {
            return reduceFunction.execute(value);
        } else {
            return defaultValue.execute();
        }
    }
}
