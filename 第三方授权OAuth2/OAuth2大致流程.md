OAuth（Open Authorization，开放授权）是一个开放标准的授权协议，允许用户授权第三方应用访问他们存储在资源服务上受保护的信息，而不需要将用户名和密码提供给第三方应用，解耦了认证和授权。OAuth作为一种国际标准，目前传播广泛并被持续采用。OAuth2.0是OAuth协议的延续版本，更加安全，更易于实现，但不向后兼容OAuth1.0，即完全废止了OAuth1.0。

这里先大概记一下OAuth2是怎么样的，后面要做一遍才知道具体。

以github为例，如果我的网站想要获得github的授权，例如github第三方登录，或者请求授权我的网站访问用户github的活动记录，统计给用户看。这需要github认证用户并由用户确认授权给我的网站，并且在这个过程中，github用户确认授权时必须在github的页面上(需要用户在github的特定页面上登录，然后告知用户要授权哪些，然后用户点确认)。OAuth2流程会避免我的网站知道用户github的密码，用户认证自身的github身份是在github的网页上(从我的网站跳转到github的提示授权页面)。github认证用户，并且用户同意授权之后，github会重定向回我的网站并通过url参数给我一个临时code，然后我后端可以通过这个code从github得到可以用来访问用户github数据的access_token。

github官方文档在<https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps>。

首先要在github上创建授权application，地址在<https://github.com/settings/applications/new>。github授权页面会提示用户是在向这个application授权。

我的网站要做的是，在用户点击时(例如点击github登录时)，跳转到<https://github.com/login/oauth/authorize>，这个URL还需要指定一些[参数](https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps#parameters)，重要的例如`client_id`是在github上注册的授权application的id，`redirect_uri`是在github这个页面用户授权之后重定向回我的网站的页面的URL，`scope`是我的授权application想要的授权范围(github会在授权页面上提示用户)。

#### Parameters

| Name           | Type     | Description                                                  |
| :------------- | :------- | :----------------------------------------------------------- |
| `client_id`    | `string` | **Required**. The client ID you received from GitHub when you [registered](https://github.com/settings/applications/new). |
| `redirect_uri` | `string` | The URL in your application where users will be sent after authorization. See details below about [redirect urls](https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps#redirect-urls). |
| `login`        | `string` | Suggests a specific account to use for signing in and authorizing the app. |
| `scope`        | `string` | A space-delimited list of [scopes](https://docs.github.com/en/apps/building-oauth-apps/understanding-scopes-for-oauth-apps). If not provided, `scope` defaults to an empty list for users that have not authorized any scopes for the application. For users who have authorized scopes for the application, the user won't be shown the OAuth authorization page with the list of scopes. Instead, this step of the flow will automatically complete with the set of scopes the user has authorized for the application. For example, if a user has already performed the web flow twice and has authorized one token with `user` scope and another token with `repo` scope, a third web flow that does not provide a `scope` will receive a token with `user` and `repo` scope. |
| `state`        | `string` | An unguessable random string. It is used to protect against cross-site request forgery attacks. |
| `allow_signup` | `string` | Whether or not unauthenticated users will be offered an option to sign up for GitHub during the OAuth flow. The default is `true`. Use `false` when a policy prohibits signups. |

在github的授权页面上，github会提示用户先用github的账号密码登录，然后提示用户我的授权application想要哪些权限，然后提示用户确认授权。

如果用户确认授权了(在github的页面上)，github会重定向到前面`redirect_uri`指定的地址，并且带着一个临时的`code`参数。当我的网站接收到指向这个`redirect_uri`的请求后，那么说明这是一个github授权完后的跳转，我的网站的后端就拿到url参数里的`code`参数，这个`code`参数是临时的，需要先向github换access token:

```
POST https://github.com/login/oauth/access_token
```

#### Parameters

| Name            | Type     | Description                                                  |
| :-------------- | :------- | :----------------------------------------------------------- |
| `client_id`     | `string` | **Required.** The client ID you received from GitHub for your OAuth App. |
| `client_secret` | `string` | **Required.** The client secret you received from GitHub for your OAuth App. |
| `code`          | `string` | **Required.** The code you received as a response to Step 1. |
| `redirect_uri`  | `string` | The URL in your application where users are sent after authorization. |

然后github默认以这样的格式返回access_token(可以用Accept字段指定以其它格式返回)

```
access_token=gho_16C7e42F292c6912E7710c838347Ae178B4a&scope=repo%2Cgist&token_type=bearer
```

这样一个流程之后，我的网站就提示了用户我想要github授权，并且用户在github的页面上点击同意了，并且我的网站拿到了可以用来访问用户github信息的access_token。

回到刚刚的`redirect_uri`的响应，如果我是用github授权登录或者注册，对于这个uri的响应就可以是先在后端从参数的`code`换到`access_token`。然后用这个`access_token`从github拿到用户的信息，用户信息里有用户的github id，检测我的数据库里是否有这个用户，然后创建用户/获取用户个人信息等等。然后通过Set-Cookie在对这个`redirect_uri`的响应里写上标识用户身份的cookie，然后前端跳转到我的网站首页。

github授权登录大概是这样，不过这个OAuth2还可以做其它类别的事，例如我的网站向用户请求授权给我获取他github活动记录，然后我的网站统计信息给他看等等。OAuth2本身的输出是我的网站拿到一个`access_token`，可以用这个`access_token`获取用户数据。

