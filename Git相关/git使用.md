### 开发代码时如何合入

​	分两种，有upstream的和无upstream的。有upstream的就是fork的别人的仓，无upstream的就是自己建的仓。

​	两种情况都要遵循一条原则，开发的代码不要直接commit到main分支，所有的开发代码的commit都在其它分支上进行，main分支用于同步。

​	代码拉下来之后，最好先

​		git remote add origin <仓库地址>

​		git remote add upstream <仓库地址>

​		git remote -v

​	设置好远程仓库地址(git remote set-url修改(需要先add)，git remote remove移除)。

---

​	对于个人的仓，要开发时先切换到这次要开发的特性的分支(以下以workbr为名)，然后在分支上开发。在新的分支上开发完成并commit之后，要先

​	git fetch origin (更新本地的远程分支，可以只fetch特定的：git fetch origin main)

​	git checkout main

​	git merge origin/main (这里写成git merge origin main不行，会提示"merge: origin - not something we can merge"，会把origin当成一个分支)

更新最新的远程分支到本地main分支(或者直接git pull origin main)，如果正常操作，这次合并不会有冲突，而是fast-forward的合并(因为本地main分支是远程main分支的子集)。然后

​	git checkout workbr

​	git rebase main (有需要的话，rebase -i，然后r，s，s...压缩commit)

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

这里还有一种情况，如果自己在一台电脑上写了一部分，然后要切换到另一台电脑。这时需要提到github才能在另一台电脑上拉下来继续。这时候最好是开一个分支，在第一台电脑上提交到这个分支，然后在第二台电脑上拉下来继续开发提交，最后完成之后用git rebase -i HEAD~n(或者main也行)压缩commit。然后在这个分支上rebase main后把这个分支合并到main分支，再git push origin main并删除开发分支。这样做的好处是不用git push -f，只需要在开的那个分支上持续提交，最后把各个阶段的commit合并成一个总的后merge到main分支，再删除开发分支就行。如果直接在main分支上提交，最后压缩commit后github上的main的commit记录会多出来，必须git push -f才能推上去，这样比较危险，万一没注意，自己本地的main上的中间的commit实际少很多，push -f后就全完了，总之不要force push。

---

对于有上游的仓，要开发时先切换到这次要开发的特性的分支(以下以workbr为名)，然后在分支上开发。在新的分支上开发完成并commit之后，要先

​	git fetch upstream (更新本地的远程分支，可以只fetch特定的：git fetch upstream main)

​	git checkout main

​	git merge upstream/main  (这里写成git merge upstream main不行，会提示"merge: upstream - not something we can merge"，会把upstream当成一个分支)

获取最新的远程上游分支(或者直接git pull upstream main)，如果正常操作，这次合并不会有冲突，而是fast-forward的合并(因为本地main分支是远程main分支的子集)。然后

​	git checkout workbr

​	git rebase main (有需要的话，rebase -i，然后r，s，s...压缩commit)

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

​	或者可以在github网页上点Sync fork，选择Update branch，然后本机再git fetch origin

最后的结果应该是：HEAD -> main,upstream/main,origin/main都在最新的commit的位置，无用的分支删除了(本地及自己的远程仓库的)，自己的远程仓库main分支刷新了。

如果直接在main上开发，那么开发完之后很可能出现上游main已经更新，推到github上之后,自己的分支不是上游分支的父集，不适合提pr(github上会提示有几个commit behind upstream，几个commit ahead of upstream)。所以就应该用main分支来和上游保持同步，开发在其它分支上进行，提pr在特性分支上进行，往远程特性分支更新时先获取最新上游main分支rebase之后再push。

---

如果没注意直接往main分支上commit了，可以先checkout -b到workbr，然后在main分支上把commit reset掉。这样效果和commit提交到开发分支相同。然后main分支仍然用于同步。

如果出现branchname和origin/branchname commit历史分叉，可以用`git pull --rebase`，或者`git fetch origin`，`git rebase origin/branchname`。(默认情况下git pull = git fetch + git merge，可以配置pull的时候默认rebase)

### git log打印出线性关系的策略
git的commit对象的parent属性里存了父commit，从而将commit历史构成一个图。当merge操作生成了新的merge commit时，这个merge commit就会有两个父commit(甚至可以更多，[git merge](https://git-scm.com/docs/git-merge#Documentation/git-merge.txt-ltcommitgt82308203)可以指定多于2个commit进行合并)。

实际使用时，**总是应当避免会生成merge commit的merge**。正确做法是先rebase，进行fast-forward的merge(只需向后移动分支指针的位置)。从而保持线性的commit历史，防止commit历史杂乱无章。

虽然实际应当保持线性历史(所以其实不用关心下面的commit历史为非线性的图时`git log`的访问策略)，不过这里有个问题是，`git log`总是以列表的形式(线性)给出commit，即便此时用`git log --graph`看commit历史是一个非线性的图。那么`git log`的访问顺序是什么？

没找到确切的说法，但是应该是BFS，然后深度相同的commit，commit时间大(较新)的先访问。

对于
```
  c1 main
 /
c0---c2(比c1新) topic
```
这样的commit图，在topic上merge main生成c3后，`git log`出来的结果是c3,c2,c1,c0。(而且看起来git内部有记实际commit的时间，merge之前`git commit --amend --date`把c2的时间设置成比c1旧之后`git log`顺序没变)。此外，从[这个链接](https://juejin.cn/post/7004009215895273486)里的内容来推测也应该是BFS。

`git log`还有个[--first-parent](https://git-scm.com/docs/git-log#Documentation/git-log.txt---first-parent)参数。`git log --first-parent`的时候只跟着first-parent打印。

first-parent指commit对象的parent属性里记录的第一个父commit。例如，上面在topic上`git merge main`，则commit历史为：
```
  c1---c3(merge commit)
 /    /
c0---c2(比c1新)
```
此时main指向c1，topic指向c3，c3的parent为c2,c1(c2为first-parent)。对于merge commit，`git log`或者`git show`的时候会多这样一行：
```
Merge: 00ea2c6 b3956c7
```
(00ea2c6为first-parent)。

### git fetch获取远程分支

git fetch origin会把所有远程分支拉下来(已有的话会更新)，git branch -a | -r能看到，没写remote默认为origin。

只fetch特定的远程分支：`git fetch origin branchname`

### git add -A和git add .微小区别

在.git所在目录下运行时是没区别的，但是实际可以在.git的子目录下运行git命令(父目录不行，运行git命令时应该是会自动往上寻找.git)，这时候git add就只会add当前目录下的所有改动，git add -A仍然是add项目的所有改动。

### git cherry-pick挑选commit

`git cherry-pick <commit>…`对commit进行挑选并生成新的commit到当前分支。可以用于将B分支的一个或几个commit也提交到A分支，例如如果不小心把内容写到了B分支里并commit了，就可以用cherry-pick将commit挑到A分支上，然后把B分支的那个commit reset掉。具体操作方式是先在B分支上找到commit id，然后checkout到A分支上`git cherry-pick commit-id-1 commit-id-2 ...`。这样做之后A分支上会生成对应的commit，但是commit id不一样(commit message一样)。

cherry-pick也可以指定区间来挑选commit，`git cherry-pick <commitId1> .. <commitId2>`可以用来挑选`(commitId1, commitId2]`区间的commit，如果想挑闭区间，这样写：`git cherry-pick <commitId1>~ .. <commitId2>`(这样如果commitId1是第一次commit，会因为commitId1~报错，不过可以用一次单独的cherry-pick解决)。

补充：[cherry-pick和rebase为什么会出现冲突](#cherry-pick和rebase为什么会出现冲突)

### github上的3种合并

参考：
* https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/about-merge-methods-on-github#rebasing-and-merging-your-commits

`Create a merge commit`会进行merge生成一个新的merge commit，如图：

![standard-merge-commit-diagram](img/standard-merge-commit-diagram.png)

效果等价于将pr分支merge到main：
```
git checkout Main
git merge Feature
```

最后会多出来3个commit对象(main上多出一个merge commit)，这个图结构可以用git log --graph看，单纯的git log不会展示旁枝。

`Squash and merge`会将commit压缩成一个，最终只会多一个commit，且没有多余的枝，如图：

![commit-squashing-diagram](img/commit-squashing-diagram.png)

效果等价于用rebase -i压缩再合并：
```
git checkout Feature
git rebase -i Main //选择把E压到D中
git checkout Main
git merge Feature //会是fast-forward的合并
```
`Rebase and merge`和在本地rebase差不多，会将特性分支上相对于基的特有的commit摘出来，生成新的commit(三路合并生成的，见[cherry-pick和rebase为什么会出现冲突](#cherry-pick和rebase为什么会出现冲突))后接到基分支后面。不过[github文档](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/about-merge-methods-on-github#rebasing-and-merging-your-commits>)里说github的rebase和git rebase略有区别，原文为:
```
Rebase and merge on GitHub will always update the committer information and create new commit SHAs, whereas git rebase outside of GitHub does not change the committer information when the rebase happens on top of an ancestor commit.
```
也即：github上的Rebase and merge总会更新committer，并生成新的commit。而对于git rebase，当rebase的目标commit位置为要rebase的分支的祖先commit时，committer信息不会变。这种情况下git rebase，git根本不需要动作，不会生成新的commit，这种情况举例来说是这样：
```
       A---B---C topic
      /
D---E---F---G master
```
这种情况下在topic上`git rebase E`，commit对象关系根本不会改变，直接就完成了。

此外，github上的pr合并之后，网页上的commit记录里能看到作者和进行合入的人(committer)。git log本身只有作者信息(看author和committer信息要用git show的--format=fuller参数)。

### 拉取github的pr

参考： 

https://blog.csdn.net/April_Lie/article/details/106554769

https://docs.github.com/cn/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/checking-out-pull-requests-locally

通过git拉取github上的Pull Request到本地 (进行review或者build和测试等，github应该是会为pr建立分支，不过不直接在项目主页的分支列表里显示)，

命令为：

​	`git fetch origin pull/PRId/head:LocalBranchName` (视具体情况，将origin替换为实际pr所属的仓库)

其中，PRId 为该 Pull Request 的序号，pull/PRId/head整体是github给这个pr建的分支名，LocalBranchName为拉取到本地后的分支名称。例如：

​	`git fetch origin pull/1204/head:pr1204`

应该也可以`git fetch <remote> pull/PRId/head`，

然后再`git checkout -b LocalBranchName <remote>/pull/PRId/head`创建本地分支

### git clone和git init + git pull微小区别

git clone会拉所有远程分支，只创建main分支。可以使用-b指定创建其它分支，不过远程分支也是都拉了。并且git clone会自动设置origin。

git init再git pull只会拉特定的分支，不会把远程分支都拉下来。也不会设置origin。

### git clone时只拉取必要内容

git clone可以使用--depth参数(必须是正整数)指定拉取的commit数量，项目内容会拉下来，但通过--depth能减少拉取的commit数量，减少.git文件夹的大小。具体使用方式是：

`git clone --depth=1 <仓库地址>`

实测有指定--depth参数时默认指定了--single-branch参数，只会拉取remote的 HEAD指向的分支。可以通过-b | --branch参数指定要clone的分支，或者--no-single-branch参数拉取所有远程分支(实测所有远程分支都拉下来了，不过本地分支只有main，还需要自己`git checkout workbr`(参考git checkout --help的文档，当origin有同名远程分支时，`git checkout workbr`相当于`git checkout -b workbr --track origin/workbr`))。例如：

`git clone --depth=1 -b <branchname> <仓库地址>`

`git clone --depth=1 --no-single-branch <仓库地址> `

git clone --help文档对--depth和--[no-]single-branch的说明如下：

```
--depth <depth>
Create a shallow clone with a history truncated to the specified number of commits. Implies --single-branch unless --no-single-branch is given to fetch the histories near the tips of all branches. If you want to clone submodules shallowly, also pass --shallow-submodules.

--[no-]single-branch
Clone only the history leading to the tip of a single branch, either specified by the --branch option or the primary branch remote’s HEAD points at. Further fetches into the resulting repository will only update the remote-tracking branch for the branch this option was used for the initial cloning. If the HEAD at the remote did not point at any branch when --single-branch clone was made, no remote-tracking branch is created.
```

### git checkout有同名远程分支时

`git checkout [<branch>]`，当有同名远程分支时，会自动以那个同名远程分支为start-point创建branch，并用--track参数设置远程分支为上游分支。不过有-b参数时：`git checkout -b <branch>`，不会尝试以同名远程分支为start-point，就是会从当前分支checkout。

git checkout --help文档对应部分如下：

---

*git checkout* [<branch>]

To prepare for working on `<branch>`, switch to it by updating the index and the files in the working tree, and by pointing `HEAD` at the branch. Local modifications to the files in the working tree are kept, so that they can be committed to the `<branch>`.

If `<branch>` is not found but there does exist a tracking branch in exactly one remote (call it `<remote>`) with a matching name and `--no-guess` is not specified, treat as equivalent to

```
$ git checkout -b <branch> --track <remote>/<branch>
```

You could omit `<branch>`, in which case the command degenerates to "check out the current branch", which is a glorified no-op with rather expensive side-effects to show only the tracking information, if exists, for the current branch.



-t

--track

When creating a new branch, set up `branch.<name>.remote` and `branch.<name>.merge` configuration entries to mark the start-point branch as "upstream" from the new branch. This configuration will tell git to show the relationship between the two branches in `git status` and `git branch -v`. Furthermore, it directs `git pull` without arguments to pull from the upstream when the new branch is checked out.

This behavior is the default when the start point is a remote-tracking branch. Set the branch.autoSetupMerge configuration variable to `false` if you want `git switch`, `git checkout` and `git branch` to always behave as if `--no-track` were given. Set it to `always` if you want this behavior when the start-point is either a local or remote-tracking branch.

---

### git show查看commit改动

查看某个commit相对于上个commit的改动，除了用`git diff <commit>~ <commit>`，也可以用`git show <commit>`。例如查看最新一个commit的改动，就可以用`git show HEAD`(或者直接`git show`，默认show HEAD)。不过git show只能diff相邻的commit，不能diff隔开的commit。此外，git show有个--format参数，可以用来查看commit的committer信息，例如：`git show HEAD --format=fuller`。(committer和author有区别，如果完全是自己的仓，author和committer都会是自己，但是如果是在github上提pr，author会是自己，committer信息会是那个pr的合入者。官方的文档只找到了[这个](https://git-scm.com/book/zh/v2/Git-%E5%9F%BA%E7%A1%80-%E6%9F%A5%E7%9C%8B%E6%8F%90%E4%BA%A4%E5%8E%86%E5%8F%B2)有说明committer的概念)

`git show <commit> --format=fuller`的输出格式为：

```
commit <hash>
Author:     <author>
AuthorDate: <author-date>
Commit:     <committer>
CommitDate: <committer-date>
<title-line>    (commit message标题)
<full-commit-message>    (commit message内容)
<textual-diff>
```

还有个--name-status参数指定只列出变更了的文件名和变更方式(不查看变更内容)：`git show --name-status`

也可以`git show <commit> <pathspec>...`查看某次提交中的文件变化

### git log查看两个分支间的commit差异
参考<https://blog.csdn.net/allanGold/article/details/87181284>，查看两个分支不同的commit有哪些，分别在哪个分支上。命令为：

`git log --left-right branch1...branch2`，会显示branch1和branch2的commit差异，输出里左箭头`<`表示commit是branch1的，右箭头`>`表示commit是branch2的。

这个`...`两侧好像不能随便加空格，试了一下左右加空格会导致结果变化(branch1 ... branch2和branch1 ...branch2只有`>`,branch1... branch2和branch1...branch2一致，奇怪)。

### git log查看历史

`git log`查看commit历史

`git log <path>…`查看文件历史

`git log -p <path>…`查看文件历史并显示diff

### git rm删除文件

`git rm`将文件从暂存区，或者暂存区和工作区中删除。

* 将文件从工作区和暂存区中删除：`git rm <pathspec>…`，若工作区和暂存区有修改，需要`git rm -f <pathspec>…`
* 将文件从暂存区中删除：`git rm --cached <pathspec>…`

### git reset进行恢复

参考链接：

https://www.jianshu.com/p/c2ec5f06cf1a

https://www.jianshu.com/p/cbd5cd504f14

https://www.runoob.com/git/git-reset.html

1. git reset能撤销之前的commit，用于撤销commit时主要分为--soft，--mixed(默认)，--hard三种模式

	* `git reset --soft HEAD^3` 只把本地仓库重置成reset目标节点的内容，看起来就像是把reset目标节点之后的更改应用到暂存区。
	* `git reset --mixed HEAD^3` --mixed是不写参数时的默认模式，把暂存区和本地仓库重置成reset目标节点的内容，看起来就像是把reset目标节点之后的更改应用到工作区。
	* `git reset --hard HEAD^3` 把工作区、暂存区、本地仓库都重置为reset目标节点的内容，**慎用**，会导致内容无法恢复。(不过就算本地和远程仓库都reset掉了，也可以使用`git reflog`找到reset之前HEAD指向的commit id，然后reset --hard回来。不过我估计reset --hard之后，被reset掉的那一枝的commit对象如果没有在其他分支上，完全成为孤儿树枝，可能会被git gc掉，然后reflog里也没有相应的记录了，恢复不回去)

2. git reset也可以用来reset某个文件，参考git文档，

```
git reset [-q] [<tree-ish>] [--] <pathspec>…
git reset [-q] [--pathspec-from-file=<file> [--pathspec-file-nul]] [<tree-ish>]
These forms reset the index entries for all paths that match the <pathspec> to their state at <tree-ish>. (It does not affect the working tree or the current branch.)

This means that git reset <pathspec> is the opposite of git add <pathspec>. This command is equivalent to git restore [--source=<tree-ish>] --staged <pathspec>...
```

使用方法例如`git reset HEAD~2 1.txt`，运行之后只reset了暂存区的1.txt，对工作区和本地仓库没影响(reset文件时没有--soft，--mixed，--hard三种模式)。

### git restore恢复文件

常用格式为: git restore [<options>] [--source=<tree>] [--staged] [--worktree] [--] <pathspec>…

git文档的一段描述为：

Restore specified paths in the working tree with some contents from a restore source. If a path is tracked but does not exist in the restore source, it will be removed to match the source.

The command can also be used to restore the content in the index with `--staged`, or restore both the working tree and the index with `--staged --worktree`.

By default, if `--staged` is given, the contents are restored from `HEAD`, otherwise from the index. Use `--source` to restore from a different commit.If `--source` not specified, the contents are restored from `HEAD` if `--staged` is given, otherwise from the index.

总的来说，用`--source`指定要reset到的目标commit，用`<pathspec>...`指定要reset的目标文件。有`--staged`参数时，会将暂存区的目标文件reset到`--source`指定的commit的状态。有`--staged`和`--worktree`参数时，会将工作区和暂存区的目标文件都reset到`--source`指定的commit的状态。

若`--source`和`--staged`都没有，才会按照暂存区恢复，否则都是按照本地仓库恢复(只写`--staged`，默认`--source`为HEAD)。

恢复某个(或某些)文件为某个commit的状态：

* 只影响工作区：`git restore --source=HEAD^3 <pathspec>...`

* 只影响暂存区：`git restore --source=HEAD^3 --staged  <pathspec>...`

* 影响工作区和暂存区：`git restore --source=HEAD^3 --staged --worktree  <pathspec>...`

恢复某个(或某些)文件为暂存区的状态：

* `git restore <pathspec>...`

### git checkout恢复文件

git checkout除了切换分支，也可以`git checkout -- <pathspec>...`恢复文件，格式为：

`*git checkout* **[-f|--ours|--theirs|-m|--conflict=<style>] [<tree-ish>] [--] <pathspec>…**`

若<tree-ish>不指定，按照暂存区恢复工作区文件。若<tree-ish>指定了，按照本地仓库恢复工作区和暂存区文件。

`git checkout -- <pathspec>...`

（相当于`git restore <pathspec>...`）

`git checkout HEAD^3 -- <pathspec>...`

（相当于`git restore --source=HEAD^3 --staged --worktree  <pathspec>...`）

### git revert提交revert commit

`git revert <commit>`会生成一个新的commit，反做指定的commit，达到撤销那个commit的效果。例如，`git revert commit-id`会生成一个新的commit，操作与指定的commit的完全相反，达到撤销指定commit的效果。git revert不会把中间的commit也反做，例如：`git revert HEAD~2`生成的revert commit只会反做HEAD^2那个commit的内容，不会把HEAD和HEAD^1的内容也反做了。

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

`git add -A`(或者`git add .`)之后再尝试`git rebase main`仍然不行

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

一般压缩的话p,s,s就可以

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

`git rebase`除了以一个别的分支为基准，也可以以当前分支为基准rebase(可以更方便地squash)，例如，在workbr上`git rebase -i HEAD~3`会列出workbr最新的3个commit以供操作(这里不写-i运行后不会有任何变化，因为默认是pick，不写-i含义是pick workbr上的最新3个commit)。`git rebase -i --root`直接从最开始的commit开始rebase。

补充：[cherry-pick和rebase为什么会出现冲突](#cherry-pick和rebase为什么会出现冲突)

### git commit的--amend参数
git commit的--amend参数能用来修改HEAD。例如`git commit --amend --date="2022-05-31T23:59:15+0800"`修改HEAD的日期，或者直接`git commit --amend`修改HEAD的commit message。

还有个简便用途是，提交了一个commit后，有一些改动，想和HEAD合成一个commit。此时可以commit后`git rebase -i HEAD~2`，也可以:

`git add -A` (必须add，--amend的操作和commit然后用rebase -i HEAD~2压缩是一样的)

`git commit --amend --no-edit` (加上--no-edit参数不会弹出commit message的editor)

合并之后commit id会变

### cherry-pick和rebase为什么会出现冲突
参考[Git底层数据结构和原理](https://www.jiqizhixin.com/articles/2020-05-20-3)：git对文件版本的管理理念是以每次提交为一次快照，提交时对所有文件做一次全量快照。

之前以为cherry-pick和rebase生成的新的commit的逻辑是直接以目标commit的状态为准，也就是说直接以目标commit的快照为新的commit的最终状态，这样按理说不会发生冲突。但是实际cherry-pick和rebase生成新的commit时是merge生成的，而不是以目标commit为最终状态。

例如：

```
       A---B---C topic
      /
D---E---F---G master
```

在topic上cherry-pick G，生成的G'的项目状态不是与G相同，而是以E为base，E,C,G进行三路合并后得到的(参考[git的三路合并与递归三路合并](https://segmentfault.com/a/1190000021712743))

再例如：

```
       A---B---C topic
      /
D---E---A'---F master
```
在topic上rebase master，变成：(这里A和A'改动相同，committer信息不同，rebase操作会skip A并提示warning，见<https://git-scm.com/docs/git-rebase>)
```
               B'---C' topic
              /
D---E---A'---F master
```
这里的过程应该是：
1. 先找到C,F的最近公共祖先E，从而找出topic相对master特有的commit是A,B,C(即(E,C])，等待将A,B,C reapply到master
2. A被skip(估计是因为E,A,F三路合并发现相对F没有修改，所以git忽略了A，如果F改了A'改动的文件，三路合并结果也应该是由F决定，A好像也能skip)
3. E,B,F三路合并成B'，接到F后面
4. E,C,B'三路合并成C'，接到B'后面
A对应的commit对象的parent列表中删除E(E---A这条边删除)，然后这里A没有其他parent commit。所以A---B---C这枝应该会被后续gc回收。

总之，cherry-pick和rebase时，新的commit是三路合并生成的，所以出现conflict是由于其中隐含的merge行为。

为什么两个人同时改同一个文件同一行，合并时会出现冲突：

base 代表公共祖先commit

ours 代表当前commit

theirs 代表要合并进来的commit

merge的时候，如果出现ours和theirs某一行的内容不相同，如果ours或者theirs的其中之一与base的这行相同，那git能知道是theirs或者ours相对base进行了修改，版本更新，会选择theirs/ours的那行内容。不过如果三者都不相同，那么就算git想抛弃base的这行，同时选择ours和theirs的这行，也没办法决定ours和theirs的两行谁在先，谁在后，只能报冲突。

此外试了下发现这种情况git不会自动合并：
```
  c1
 /
c0---c2

c0的test.txt为空

c1的test.txt为:
unit1
block1
unit2
unit3

c2的test.txt为:
unit1
unit2
block2
unit3
```
这里unit代表相同的块(git应该是用最长公共子序列算法找公共部分)，block代表不同的块(增加或删除行)。

原本以为c1和c2能自动merge为：
```
unit1
block1
unit2
block2
unit3
```
但是实际是报冲突，具体的合并的逻辑不知道是什么样的。(不过这里c0的test.txt要是和c1内容一样的话c1,c2肯定能自动merge)。

这种情况：
```
  c1
 /
c0---c2

c0的test.txt为:
a
b
c
d

c1的test.txt为:
1
a
b
c
d

c2的test.txt为:
a
b
c
d
2
```
c1,c2还是能自动merge的：
```
1
a
b
c
d
2
```
这样一来，或许可以猜测一下git合并两个commit同一文件的逻辑。

首先，找到两个commit的base后，用最长公共子序列算法找公共部分，这里记找出来的公共块为unit1,unit2,...

然后这些公共块把要合并的文件进行了分隔，从而可以按块对齐：

commit1的某个文件：block1 unit1 block2 unit2 block3 unit3 ...

commit2的对应文件：block1' unit1 block2' unit2 block3' unit3 ...

base的对应文件：block1^ unit1 block2^ unit2 block3^ unit3 ...

(blocki,blocki',blocki^可能为空)

然后现在就是要根据blocki^决定blocki,blocki'的取舍问题。

* 若blocki与blocki'相同，则合并后取blocki，blocki'任一

* 否则(blocki与blocki'不同)：
  - 若blocki/blocki'与blocki^相同，那么合并要取的应该就是blocki'/blocki。
  - 若blocki,blocki',blocki^两两不相同，此时报conflict，由用户对blocki,blocki'进行选择

这样一来，上面那个
```
  c1
 /
c0---c2

c0的test.txt为空

c1的test.txt为:
unit1
block1
unit2
unit3

c2的test.txt为:
unit1
unit2
block2
unit3
```
不能自动合并的原因就能理解了，上面的uniti理解为了c1,c2的公共块，实际git应该是要取c0,c1,c2三者的公共块，所以上面的例子实际uniti为空，按每行为一个block进行对齐，遇到c1,c2的同一行不相同就会报conflict。
### ubuntu下设置git默认编辑器为VSCode

ubuntu下git默认的编辑器为GNU nano(例如`git commit`时的默认编辑器)，不太方便，windows下似乎是安装时会提示进行选择。要设置git默认编辑器为VSCode，运行`git config --global core.editor "code --wait"`，或者直接编辑~/.gitconfig，同样修改code.editor，例如：

```
[core]
	quotepath = false
	editor = code --wait
```

