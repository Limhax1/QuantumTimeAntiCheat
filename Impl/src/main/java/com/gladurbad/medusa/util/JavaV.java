package com.gladurbad.medusa.util;

import java.util.stream.*;
import javax.annotation.*;
import java.util.*;

public class JavaV
{
    @SafeVarargs
	public static <T> Stream<T> stream(final T... array) {
        return Arrays.stream(array);
    }
    
    public static <T> T firstNonNull(@Nullable final T t, @Nullable final T t2) {
        return (t != null) ? t : t2;
    }
    
    public static <T> Queue<T> trim(final Queue<T> queue, final int n) {
        for (int i = queue.size(); i > n; --i) {
            queue.poll();
        }
        return queue;
    }
}
