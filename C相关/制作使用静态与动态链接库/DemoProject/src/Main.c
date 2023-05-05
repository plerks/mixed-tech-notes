#include <stdio.h>
#include "Average.h"

int main(int argc, char *argv) {
    int a = 10;
    int b = 7;
    printf("add(a, b): %d\n", add(a, b));
    printf("subtract(a, b): %d\n", subtract(a, b));
    printf("average(a, b): %d\n", average(a, b));
}