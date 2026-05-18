package com.example.doshka.di

import com.example.doshka.BuildConfig
import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.data.remote.dto.RefreshTokenRequest
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(dataStoreManager: DataStoreManager): AuthInterceptor {
        return AuthInterceptor(dataStoreManager)
    }

    /**
     * Інтерсептор для динамічної зміни BASE_URL
     * Читає актуальний URL з DataStore при кожному запиті
     */
    @Provides
    @Singleton
    fun provideDynamicUrlInterceptor(dataStoreManager: DataStoreManager): DynamicUrlInterceptor {
        return DynamicUrlInterceptor(dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        dataStoreManager: DataStoreManager,
        json: Json
    ): TokenAuthenticator {
        return TokenAuthenticator(dataStoreManager, json)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        dynamicUrlInterceptor: DynamicUrlInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        // Використовуємо placeholder URL, бо реальний URL підставляється в DynamicUrlInterceptor
        return Retrofit.Builder()
            .baseUrl("http://placeholder.local/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideDoshkaApi(retrofit: Retrofit): DoshkaApi {
        return retrofit.create(DoshkaApi::class.java)
    }
}

/**
 * Інтерсептор аутентифікації
 * Читає токен синхронно при кожному запиті для гарантії актуальності
 */
class AuthInterceptor(
    private val dataStoreManager: DataStoreManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        // Читаємо токен синхронно через runBlocking
        // DataStore використовує внутрішній кеш, тому це швидко
        val token = kotlinx.coroutines.runBlocking {
            dataStoreManager.accessToken.first()
        }

        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}

/**
 * Інтерсептор, що підставляє актуальний URL сервера з налаштувань
 * Використовує кешований URL щоб уникнути блокування UI
 */
class DynamicUrlInterceptor(
    dataStoreManager: DataStoreManager
) : Interceptor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serverUrlRef = AtomicReference(DataStoreManager.DEFAULT_SERVER_URL)

    init {
        // Підписуємось на зміни URL асинхронно
        dataStoreManager.serverUrl
            .onEach { url -> serverUrlRef.set(url) }
            .launchIn(scope)
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()

        // Використовуємо кешований URL
        val serverUrl = serverUrlRef.get()

        // Парсимо URL сервера
        val baseUrl = serverUrl.toHttpUrl()

        // Замінюємо host та scheme в оригінальному запиті
        val newUrl = originalRequest.url.newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}

/**
 * Authenticator для автоматичного оновлення токена при 401
 */
class TokenAuthenticator(
    private val dataStoreManager: DataStoreManager,
    private val json: Json
) : Authenticator {

    override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
        // Якщо це вже повторна спроба або запит на refresh — не повторюємо
        if (response.request.header("Authorization-Retry") != null) {
            Timber.w("Повторна спроба refresh не вдалася, потрібна повторна авторизація")
            return null
        }

        // Пропускаємо запити на login/register/refresh
        val path = response.request.url.encodedPath
        if (path.contains("/auth/login") ||
            path.contains("/auth/register") ||
            path.contains("/auth/refresh")) {
            return null
        }

        Timber.d("Отримано 401, намагаємося оновити токен")

        return runBlocking {
            try {
                val refreshToken = dataStoreManager.refreshToken.first()
                if (refreshToken.isNullOrBlank()) {
                    Timber.w("Refresh token відсутній")
                    dataStoreManager.clearTokens()
                    return@runBlocking null
                }

                // Робимо запит на оновлення токена напряму через OkHttp
                val serverUrl = dataStoreManager.serverUrl.first()
                val refreshUrl = "${serverUrl}api/v1/auth/refresh"

                val requestBody = json.encodeToString(
                    RefreshTokenRequest.serializer(),
                    RefreshTokenRequest(refreshToken)
                ).toRequestBody("application/json".toMediaType())

                val refreshRequest = Request.Builder()
                    .url(refreshUrl)
                    .post(requestBody)
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val refreshResponse = client.newCall(refreshRequest).execute()

                if (refreshResponse.isSuccessful) {
                    val responseBody = refreshResponse.body?.string()
                    if (responseBody != null) {
                        val tokens = json.decodeFromString<TokenResponseDto>(responseBody)

                        // Зберігаємо нові токени
                        dataStoreManager.saveTokens(
                            tokens.accessToken,
                            tokens.refreshToken,
                            tokens.expiresIn
                        )

                        Timber.d("Токен успішно оновлено")

                        // Повторюємо оригінальний запит з новим токеном
                        return@runBlocking response.request.newBuilder()
                            .removeHeader("Authorization")
                            .addHeader("Authorization", "Bearer ${tokens.accessToken}")
                            .addHeader("Authorization-Retry", "true")
                            .build()
                    }
                }

                Timber.w("Не вдалося оновити токен: ${refreshResponse.code}")
                // Очищаємо авторизацію при невдалому refresh
                if (refreshResponse.code == 401 || refreshResponse.code == 403) {
                    dataStoreManager.clearTokens()
                }
                null
            } catch (e: Exception) {
                Timber.e(e, "Помилка при оновленні токена")
                null
            }
        }
    }
}

/**
 * DTO для відповіді токена (використовується в Authenticator)
 */
@kotlinx.serialization.Serializable
private data class TokenResponseDto(
    @kotlinx.serialization.SerialName("access_token")
    val accessToken: String,
    @kotlinx.serialization.SerialName("refresh_token")
    val refreshToken: String,
    @kotlinx.serialization.SerialName("expires_in")
    val expiresIn: Long,
    @kotlinx.serialization.SerialName("token_type")
    val tokenType: String = "bearer"
)
