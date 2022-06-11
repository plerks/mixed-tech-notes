### 开发代码时如何合入

​	分两种，有upstream的和无upstream的。有upstream的就是fork的别人的仓，无upstream的就是自己建的仓。

​	两种情况都要遵循一条原则，开发的代码不要直接commit到main分支，所有的开发代码的commit都在其它分支上进行，main分支用于同步。

​	代码拉下来之后，最好先

​		git remote add origin <仓库地址>

​		git remote add upstream <仓库地址>

​		git remote -v

​	设置好远程仓库地址。

---

​	对于个人的仓，要开发时先切换到这次要开发的特性的分支(以下以workbr为名)，然后在分支上开发。在新的分支上开发完成并commit之后，要先

​		git fetch origin (更新本地的远程分支)

​		git checkout main

​		git merge origin/main (这里写成git merge origin main不行，会提示"merge: origin - not something we can merge"，会把origin当成一个分支)

更新最新的远程分支到本地main分支(或者直接git pull origin main)，如果正常操作，这次合并不会有冲突，而是fast-forward的合并(因为本地main分支是远程main分支的子集)。然后

​	git checkout workbr

​	git rebase main

​	git checkout main

​	git merge workbr  (fast-forward的合并)

​	git push orgin main

这样就将开发完成的代码合并到本地main分支并同步到远程仓。合并之后特性分支就没用了，然后

​	git branch -d workbr

​	git push origin --delete workbr 或 git push origin -d workbr

分别删除本地和远程的特性分支。

最后，此时最好再git fetch origin一次，更新本地远程分支，让HEAD -> main,origin/main都在最新的commit的位置。

(也可以先push到远程开发分支，然后再在仓内向main分支提pr再合并，不过自己一个人的仓的话直接按上面做就可以了)

假如直接在main上开发，如果自己同时在两台电脑上在开发(比如双系统)，在某一时刻电脑A,B和远程的main分支相同，然后在电脑A上进行了一次提交并同步到远程仓库，然后这时在电脑B上进行一次commit，这时如果要提交会出现冲突(也就是提交时远程已经进行了更新的问题)。要解决的话就得先把远程分支拿回来rebase再push，所以就应该用main分支来和远程保持同步，开发在其它分支上进行，往远程main更新时先获取最新远程main分支rebase之后再push。

这里还有一种情况，如果自己在一台电脑上写了一部分，然后要切换到另一台电脑。这时需要提到github才能在另一台电脑上拉下来继续。这时候最好是开一个分支，在第一台电脑上提交到这个分支，然后在第二台电脑上拉下来继续开发提交，最后完成之后用git rebase -i或者git reset --mixed再commit，把commit合并。然后在这个分支上rebase main后把这个分支合并到main分支，再git push origin main并删除开发分支。这样做的好处是不用git push -f，只需要在开的那个分支上持续提交，最后把各个阶段的commit合并成一个总的后merge到main分支，再删除开发分支就行。如果直接在main分支上提交，最后压缩commit后github上的main的commit记录会多出来，必须git push -f才能推上去，这样比较危险，万一没注意，自己本地的main上的中间的commit实际少很多，push -f后就全完了，总之不要force push。

---

对于有上游的仓，要开发时先切换到这次要开发的特性的分支(以下以workbr为名)，然后在分支上开发。在新的分支上开发完成并commit之后，要先

​	git fetch upstream (更新本地的远程分支)

​	git checkout main

​	git merge upstream/main  (这里写成git merge upstream main不行，会提示"merge: upstream - not something we can merge"，会把upstream当成一个分支)

获取最新的远程上游分支(或者直接git pull upstream main)，如果正常操作，这次合并不会有冲突，而是fast-forward的合并(因为本地main分支是远程main分支的子集)。然后

​	git checkout workbr

​	git rebase main

​	git push orgin workbr

这样就先将main与最新的上游同步一次，然后以main为基准将自己在分支上的更改rebase,将特性分支push到远程仓之后(例如github),去github上提pr(这里要么让上游开一个特性分支，然后从origin/workbr向上游开的upstream/特性分支上提pr，上游合并到他开的特性分支后可以拉下来进行测试，测试好了然后上游再合到他的main分支;要么直接往upstream/main上提pr,上游把pr拉下来测试),等pr被合并之后，

先更新本地的upstream和main：

​	git fetch upstream

​	git checkout main

​	git merge upstream/main

​	(或者直接git pull upstream main)

再删除无用的特性分支：

​	git branch -d workbr

​	git push origin --delete workbr 或 git push origin -d workbr

这里有一个问题是，上游如果用rebase and merge(其它两种合并不知道)，那么合并到上游的commit的commit id会变化，当git branch -d workbr时，自己提交的那个commit在其它分支上没有(即使main分支已经从上游更新了自己提的那个commit，但是commit id不一样)，删除了后会导致丢失commit，所以git会让这个操作失败，并提示'The branch workbr is not fully merged'，需要用git branch -D workbr。

然后再将自己的远程仓库和本地的origin刷新为最新的：

​	git push origin main (从本地的main上推)

​	git fetch origin

​	或者可以在github上点Fetch upstream，然后git fetch origin

最后的结果应该是：HEAD -> main,upstream/main,origin/main都在最新的commit的位置，无用的分支删除了(本地及自己的远程仓库的)，自己的远程仓库main分支刷新了。

如果直接在main上开发，那么开发完之后很可能出现上游main已经更新，推到github上之后,自己的分支不是上游分支的父集，不适合提pr(github上会提示有几个commit behind upstream，几个commit ahead of upstream)。所以就应该用main分支来和上游保持同步，开发在其它分支上进行，提pr在特性分支上进行，往远程特性分支更新时先获取最新上游main分支rebase之后再push。

---

​	如果没注意直接往main分支上commit了，可以先checkout -b到workbr，然后在main分支上把commit reset掉。这样效果和commit提交到开发分支相同。然后main分支仍然用于同步。

### github上的3种合并

参考：

```
https://docs.github.com/cn/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/about-merge-methods-on-github
https://www.chenshaowen.com/blog/the-difference-of-tree-ways-of-merging-code-in-github.html
https://www.runoob.com/git/git-commit-history.html
```



`Create a merge commit`会合并commit并生成一个新的merge commit，如图：

![standard-merge-commit-diagram](https://docs.github.com/assets/cb-5407/images/help/pull_requests/standard-merge-commit-diagram.png)

最后main上面会多出来3个commit，这个树形结构可以用git log --graph看

`Squash and merge`会将commit压缩成一个，最终只会多一个commit，且没有多余的分支信息（感觉像是github先通过git rebase -i压缩了commit，再合并），如图：

![commit-squashing-diagram](https://docs.github.com/assets/cb-5742/images/help/pull_requests/commit-squashing-diagram.png)

`Rebase and merge`会将特性分支上相对于基底的新的commit摘出来，接到基底分支后面。但参考链接https://docs.github.com/cn/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/about-merge-methods-on-github说github的rebase和git rebase略有区别，还没研究。rebase and merge合并之后，下游提来的commit的commit id会变化。

此外，github上的pr合并之后，commit记录里能看到作者和进行合入的人。git log本身只有作者信息。

### github拉取pr

参考： 

https://blog.csdn.net/April_Lie/article/details/106554769

https://docs.github.com/cn/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/checking-out-pull-requests-locally

通过 Git 拉取 github 上的 Pull Request(PR) 到本地进行 review：

如果自己使用的就是PR提交的仓库，命令为：

​	`git fetch origin pull/PRId/head:LocalBranchName`
其中，PRId 为该 Pull Request 的序号，pull/PRId/head是github给这个pr建的分支，LocalBranchName为拉取到本地后的分支名称。

​	`git fetch origin pull/1204/head:pr1204`
类似，如果自己的仓库是Clone的PR提交的仓库，则命令为：

​	`git fetch upstream pull/PRId/head:LocalBranchName`

### git reset进行恢复

参考链接：

https://www.jianshu.com/p/c2ec5f06cf1a

https://www.jianshu.com/p/cbd5cd504f14

https://www.runoob.com/git/git-reset.html

git reset能撤销之前的commit，分为--soft，--mixed(默认)，--hard三种模式

* `git reset --soft HEAD^3` 只把本地仓库重置成reset目标节点的内容，看起来就像是把reset目标节点之后的更改应用到暂存区。

* `git reset --mixed HEAD^3` --mixed是不写参数时的默认模式，把暂存区和本地仓库重置成reset目标节点的内容，看起来就像是把reset目标节点之后的更改应用到工作区。
* `git reset --hard HEAD^3` 把工作区、暂存区、本地仓库都重置为reset目标节点的内容，**慎用**，会导致内容无法恢复。

### git revert提交revert commit

`git revert <commit>`会生成一个新的commit，反做指定的commit，达到撤销那个commit的效果。例如，`git revert commit-id`会生成一个新的commit，操作与指定的commit的完全相反，达到撤销指定commit的效果。git revert不会把中间的commit也反做，例如：`git revert HEAD~2`生成的revert commit只会反做HEAD~2那个commit的内容，不会把HEAD和HEAD~1的内容也反做了。

git revert应该用在对远程仓库的撤销，例如远程仓库main分支上有一个不想要的commit要撤销。一种方式是在本地reset去除这个commit，然后push -f(github上没有直接删除某个commit的功能)，但是这样比较危险，如果没注意其实本地差了很多commit就完了，而且对其它使用者和下游也不友好。另一种方式就是使用`git revert <commit>`。

如果还未推到远程仓库，这时在确认安全后，使用git reset撤销即可。但是如果已经推到了远程仓库，这时就只能使用git revert了(除非想使用危险的push -f)。

### git stash保存和恢复进度

参考链接：

https://blog.csdn.net/daguanjia11/article/details/73810577

https://www.jianshu.com/p/c2ec5f06cf1a

https://www.jianshu.com/p/cbd5cd504f14

https://www.runoob.com/git/git-reset.html

对于以下情景：自己在workbr工作区(和暂存区)有一些内容，现在上游进行了更新，需要拉到本地。直接把上游更新到main然后在workbr上rebase main会提示需要提交：

如果工作区有内容会提示

```
$ git rebase main
error: cannot rebase: You have unstaged changes.
error: Please commit or stash them.
```

`git add -A`之后再尝试`git rebase main`仍然不行

```
$ git rebase main
error: cannot rebase: Your index contains uncommitted changes.
error: Please commit or stash them.
```

现在要进行rebase，但是不想直接进行提交(因为还没做完)

一种办法是先commit再rebase，然后

`git reset --soft HEAD`(只把本地仓库重置成reset目标节点的内容，看起来就像是把reset目标节点之后的更改应用到暂存区)

或

`git reset --mixed HEAD`(--mixed是不写参数时的默认模式，把暂存区和本地仓库重置成reset目标节点的内容，看起来就像是把reset目标节点之后的更改应用到工作区)

把commit的内容返还(注意不能`git reset --hard HEAD`，这会导致工作区、暂存区、本地仓库都重置为reset目标节点位置的内容，更改就丢失了)。

更直接的办法是使用**git stash**保存这次的进度

**git stash**会把工作区和暂存区的内容保存起来(使用`git stash save 'message'`可以添加注释)

**git stash list**可以显示stash保存的进度的列表

**git stash pop [–index] [stash_id]**

* `git stash pop`把之前保存的最新一次的进度恢复到工作区。会把保存的工作区和暂存区的改动都恢复到工作区。

* `git stash pop --index`把之前保存的最新一次的进度恢复到工作区和暂存区。会把保存的工作区改动恢复到工作区，暂存区的改动恢复到暂存区。

* `git stash pop stash@{1}`恢复指定的进度到工作区。stash_id是通过git stash list命令得到的。

通过git stash pop命令恢复进度后，会删除当前进度。

**git stash apply [–index] [stash_id]**

除了不删除恢复的进度之外，其余和`git stash pop` 命令一样。

**git stash drop [stash_id]**

删除一个存储的进度。如果不指定stash_id，则默认删除最新的存储进度。

`git stash clear`

删除所有存储的进度

具体来说：先`git stash`把目前的工作区暂存区内容都保存起来，stash之后工作区和暂存区都是干净的，没有任何改动，然后再进行rebase，rebase完成后`git stash pop`或`git stash pop --index`把保存的进度恢复就可以继续进行了。总之，`git stash`可以用于需要暂存工作区暂存区内容，然后后续再恢复的场景。

### 设置跟踪的上游分支

一般git push时需要指定远程仓库地址和分支，如`git push origin workbr`。通过设置默认跟踪的上游分支，可以只运行`git push`就可以推到跟踪的上游分支。具体做法是：

* 方法一：

​    在push的时候使用-u或者--set-upstream参数，如`git push -u origin workbr`或者`git push --set-upstream origin workbr`。设置的时候一般本地远程分支同名相对，不要设置成workbr跟踪origin main。

* 方法二：

​    git branch --set-upstream-to=origin/<远程分支> <本地分支>，本地分支如果不写默认为当前所在的分支

**取消设置跟踪的上游分支**：

`git branch --unset-upstream`

**查看上游分支**：

`git branch -vv`能看到本地分支的上游分支；运行`git status` 、`git checkout <branchname>`，如果分支设置了上游分支，也能在输出中看出来上游分支是什么：

```
$ git status
On branch main
Your branch is ahead of 'origin/main' by 1 commit.
  (use "git push" to publish your local commits)

nothing to commit, working tree clean
```

### 清理本地的远程分支

`git branch -a`和`git branch -r` 能看到本地的远程分支。如果用`git push -d origin <branchname>`，本地分支、本地的远程分支、远程仓库分支都会被删除。但是如果直接在github上删除分支然后手动删除本地分支的话，本地的远程分支不会被删除，而且单纯`git fetch`也不会删除这些远程分支。例如：

```
$ git branch -a
* main
  remotes/origin/HEAD -> origin/main
  remotes/origin/main
  remotes/origin/test
  remotes/origin/update
  remotes/upstream/main

```

解决办法是：

* 运行`git remote prune origin`，这会清理多余的本地远程分支(远程仓库没有的，`git remote --help`相应原文为"but depending on global configuration and the configuration of the remote we might even prune local tags that haven't been pushed there.")
* 运行`git fetch -p origin` 或者`git fetch --prune origin`，这会在fetch的同时清理多余的本地远程分支(远程仓库没有的，`git fetch --help`相应原文为"Before fetching, remove any remote-tracking references that no longer exist on the remote")

还有一个命令是`git fetch -P <name>`或者`git fetch --prune-tags <name>`，这个参数会清理多余的git tag生成的标签(`git fetch --help`相应原文为"Before fetching, remove any local tags that no longer exist on the remote if --prune is enabled.")。这意味着这个-P参数必须有-p(或--prune)在场才会生效(`git fetch -p -P origin`)，实测-P写在-p前面也可以。

### git rebase -i合并commit

有时在自己本地开发一个功能时可能会在中间过程中多次commit，最后提交时需要将这些commit合并成一个。一种办法是使用 git reset --mixed | --soft 重置然后重新commit，更好的方式是用git rebase -i来合并commit。

例如，workbr分支上最新3个commit是相对main分支上多的，要将这3个commit压缩成一个：

```
$ git log --oneline
13ea7f9 (HEAD -> test) add User.java
133a449 add Controller.java
c88e398 modify Test.java
5a7d6d7 (main) add README.md
b881b80 add Test.java
```

先在workbr分支上`git rebase -i main`，git会将多出来的commit列出，全部pick也是不写-i参数时rebase的默认行为:

```
pick c88e398 modify Test.java
pick 133a449 add Controller.java
pick 13ea7f9 add User.java
```

从上到下分别是旧到新的commit，按照git给出的提示使用squash向上压缩(只能把新的commit压缩到旧的里，不能把旧的压缩到新的里)，并且将commit c88e398的message进行修改:

```
r c88e398 modify Test.java
s 133a449 add Controller.java
s 13ea7f9 add User.java
```

保存之后会弹出来一个新的编辑页面:

```
modify Test.java
```

将内容改成自己想要的message即可:

```
finish version 1.0
```

这一步是git记录c88e398这个commit的commit message要reword成什么，保存之后又会弹出来一个新的编辑页面：

```
# This is a combination of 3 commits.
# This is the 1st commit message:

finish version 1.0

# This is the commit message #2:

add Controller.java

# This is the commit message #3:

add User.java
```

这次是最终的commit的message，修改message信息即可：

```
# This is a combination of 3 commits.
# This is the 1st commit message:

finish version 1.0
```

保存之后就完成了commit的压缩：

```
$ git log --oneline
d484656 (HEAD -> workbr) finish version 1.0
5a7d6d7 (main) add README.md
b881b80 add Test.java
```

**这里还会有一个小问题**，最终生成了finish version 1.0这个commit，但是git log查看commit日期是和modify Test.java这个commit相同的，时间并不会刷新为当前rebase的时间，有可能modify Test.java距离最终压缩成finish version 1.0已经过去很多天了。为了更新finish version 1.0的commit时间，运行`git commit --amend --date="2022-05-31T23:59:15+0800"`修改最新一个commit的时间为当前做完时候的日期(还没找到修改任一个指定commit的日期的指令，这个指令只能修改最新的那个commit的日期)。

**补充说明：** 有时候如果是向上游提交pr的话，这个finish version 1.0被上游合并之后上游那里合进去的是个github新生成的commit，日期是被刷新了的，所以不改日期也行，同步上游的就行了。但是如果是自己的仓内把这个commit往main分支上提pr合，不会生成新的commit，还是要修改日期。还有github的commit记录里显示的时间是commit被提交到github上的时候的时间，和git log看到的commit的时间不一样。

