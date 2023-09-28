package Java相关.多级条件排序写法;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultipleConditionSortDemo {
    public static void main(String[] args) {
        List<Promotion> list = new ArrayList<>();
        list.add(new Promotion(5, 6, 1));
        list.add(new Promotion(3, 4, 0));
        list.add(new Promotion(1, 3, 2));
        Collections.sort(list, (x, y) -> {
            if (x.discount != y.discount) {
                return x.discount > y.discount ? -1 : 1;
            }
            if (x.endDate != y.endDate) {
                return x.endDate < y.endDate ? -1 : 1;
            }
            if (x.id != y.id) {
                return x.id < y.id ? -1 : 1;  // 直接写return x.id - y.id可能会有Integer.MIN_VALUE - 1 > 0导致排序错误的问题
            }
            return 0;
        });
        for (Promotion promotion : list) {
            System.out.println(String.format("discount: %s, endDate: %s, id: %s", promotion.discount, promotion.endDate, promotion.id));
        }
    }
}

class Promotion {
    int discount;
    int endDate;
    int id;

    public Promotion(int discount, int endDate, int id) {
        this.discount = discount;
        this.endDate = endDate;
        this.id = id;
    }
}