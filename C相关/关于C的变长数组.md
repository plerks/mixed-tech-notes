这里的C变长数组，不是指数组要在运行时改变大小，而是在运行时才能确定大小的栈区数组，数组的大小由变量指定（局部变量或者参数）。

C通过char a[7];这样声明的数组会在栈帧里面。

C99标准引入了变长数组，较早版本的C编译器要求声明数组时数组大小必须是常量。

变长数组的特点是编译时不能确定栈帧大小。以这个为例看下生成的汇编代码：
```C
#include <stdio.h>

int testArr() {
    int n = 7;
    char a[n];
    a[0] = 1;
}
```
windows下用mingw-w64的gcc生成汇编代码，`gcc --version`:
```
gcc.exe (x86_64-win32-seh-rev0, Built by MinGW-W64 project) 8.1.0
Copyright (C) 2018 Free Software Foundation, Inc.
This is free software; see the source for copying conditions.  There is NO
warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
```
`gcc -S 文件名 -masm=intel`生成的汇编代码如下：
```x86asm
	.file	"arr.c"
	.intel_syntax noprefix
	.text
	.globl	testArr
	.def	testArr;	.scl	2;	.type	32;	.endef
	.seh_proc	testArr
testArr:
	push	rbp
	.seh_pushreg	rbp
	mov	rbp, rsp
	.seh_setframe	rbp, 0
	sub	rsp, 32
	.seh_stackalloc	32
	.seh_endprologue
	mov	rax, rsp
	mov	rcx, rax
	mov	DWORD PTR -4[rbp], 7
	mov	eax, DWORD PTR -4[rbp]
	movsx	rdx, eax
	sub	rdx, 1
	mov	QWORD PTR -16[rbp], rdx
	movsx	rdx, eax
	mov	r10, rdx
	mov	r11d, 0
	movsx	rdx, eax
	mov	r8, rdx
	mov	r9d, 0
	cdqe
	add	rax, 15
	shr	rax, 4
	sal	rax, 4
	call	___chkstk_ms
	sub	rsp, rax
	mov	rax, rsp
	add	rax, 0
	mov	QWORD PTR -24[rbp], rax
	mov	rax, QWORD PTR -24[rbp]
	mov	BYTE PTR [rax], 1
	mov	rsp, rcx
	nop
	mov	rsp, rbp
	pop	rbp
	ret
	.seh_endproc
	.ident	"GCC: (x86_64-win32-seh-rev0, Built by MinGW-W64 project) 8.1.0"

```
-4[rbp]的位置是局部变量n的位置，然后`mov eax, DWORD PTR -4[rbp]`将n的值移到eax，然后`sub	rsp, rax`为char数组留了空间（在栈帧），大概应该是这样。也就是说变长数组直接移rsp就行了好像。