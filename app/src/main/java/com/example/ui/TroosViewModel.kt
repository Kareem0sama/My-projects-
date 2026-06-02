package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TroosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TroosDatabase.getDatabase(application)
    private val dao = database.dao()
    private val geminiRepository = GeminiRepository()

    // 1. Core State Flows
    val allProducts: StateFlow<List<ProductEntity>> = dao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItemEntity>> = dao.getCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myBikes: StateFlow<List<BikeProfileEntity>> = dao.getMyBikes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communityPosts: StateFlow<List<CommunityPostEntity>> = dao.getCommunityPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ordersList: StateFlow<List<OrderEntity>> = dao.getOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Filter & Compatibility State Flows for search/shop
    private val _selectedBrand = MutableStateFlow("")
    val selectedBrand = _selectedBrand.asStateFlow()

    private val _selectedModel = MutableStateFlow("")
    val selectedModel = _selectedModel.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear = _selectedYear.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts, _selectedBrand, _selectedModel, _selectedYear, _searchQuery
    ) { products, brand, model, year, search ->
        products.filter { p ->
            val matchBrand = brand.isEmpty() || p.brand.equals(brand, ignoreCase = true)
            val matchModel = model.isEmpty() || p.model.equals(model, ignoreCase = true)
            val matchYear = year == null || p.year == year
            val matchSearch = search.isEmpty() || 
                    p.name.contains(search, ignoreCase = true) || 
                    p.arabicName.contains(search, ignoreCase = true) ||
                    p.category.contains(search, ignoreCase = true)
            
            matchBrand && matchModel && matchYear && matchSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. User B2B workshop activation state
    private val _isB2BMode = MutableStateFlow(false)
    val isB2BMode = _isB2BMode.asStateFlow()

    // Workshop special profile & tiers
    private val _workshopLoyaltyPoints = MutableStateFlow(2450)
    val workshopLoyaltyPoints = _workshopLoyaltyPoints.asStateFlow()

    // 4. AI Chat / Diagnostics State
    private val _aiChatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf(Pair("أهلاً بك في مساعد تروس الميكانيكي الذكي! أنا هنا لمساعدتك في فحص الموتوسيكل الخاص بك وتحديد قطع الغيار المتوافقة. ما المشكلة التي تواجهها اليوم؟", false))
    )
    val aiChatHistory = _aiChatHistory.asStateFlow()

    private val _aiDiagnosticResult = MutableStateFlow<String?>(null)
    val aiDiagnosticResult = _aiDiagnosticResult.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    // 5. Value added services states
    private val _activeRoadsideRescue = MutableStateFlow(false)
    val activeRoadsideRescue = _activeRoadsideRescue.asStateFlow()

    private val _activeMobileMechanicRequest = MutableStateFlow<String?>(null) // e.g. "REQUEST_PENDING"
    val activeMobileMechanicRequest = _activeMobileMechanicRequest.asStateFlow()

    init {
        // Prepopulate products database if empty
        viewModelScope.launch {
            allProducts.first().let { current ->
                if (current.isEmpty()) {
                    val initialProducts = listOf(
                        ProductEntity(
                            name = "Honda CBR Front Brembo Brake Pads",
                            arabicName = "تيل فرامل بريمبو أمامى فئة CBR",
                            category = "فرامل",
                            price = 1250.00,
                            oldPrice = 1400.00,
                            imageUrl = "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format",
                            brand = "Honda",
                            model = "CBR 150",
                            year = 2022,
                            description = "تيل فرامل رياضي أصلي ماركة Brembo يقدم كفاءة توقف استثنائية وعمر افتراضي طويل تحت الضغوط الحرارية لـ CBR 150.",
                            stock = 15,
                            isOffer = true
                        ),
                        ProductEntity(
                            name = "Honda CBR Performance Oil Filter",
                            arabicName = "فلتر زيت رياضي K&N أصلي لـ CBR",
                            category = "فلاتر",
                            price = 550.00,
                            imageUrl = "https://images.unsplash.com/photo-1542282088-72c9c27ed0cd?auto=format",
                            brand = "Honda",
                            model = "CBR 150",
                            year = 2022,
                            description = "فلتر زيت عالي التدفق مع صمام أمان مدمج، يضمن حماية المحرك وتوصيل مثالي لزيت هوندا سينثيتيك.",
                            stock = 25
                        ),
                        ProductEntity(
                            name = "Yamaha R6 Laser Iridium NGK Spark Plugs",
                            arabicName = "طقم بوجيهات ليزر إيريديوم ياماها R6",
                            category = "كهرباء",
                            price = 2800.00,
                            oldPrice = 3300.00,
                            imageUrl = "https://images.unsplash.com/photo-1449426468159-d96dbf08f19f?auto=format",
                            brand = "Yamaha",
                            model = "R6",
                            year = 2020,
                            description = "بوجيهات NGK Laser Iridium لسرعة استجابة المحرك وتعديل توقيت الشرارة، مما يزيد من إنتاجية الـ R6 الحصانية.",
                            stock = 9,
                            isOffer = true
                        ),
                        ProductEntity(
                            name = "Yamaha R6 Michelin Pilot Tyre Set",
                            arabicName = "طقم إطارات ميشلان بايلوت R6 كاب",
                            category = "إطارات",
                            price = 11500.00,
                            imageUrl = "https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format",
                            brand = "Yamaha",
                            model = "R6",
                            year = 2020,
                            description = "طقم إطارات ميشلان بايلوت توفر ثباتاً ممتازاً في المنعطفات الحادة على سرعات الحلبات وخيار ممتاز للشارع والمضمار.",
                            stock = 4
                        ),
                        ProductEntity(
                            name = "DID Pro Gold Drive Chain Set Pulsar 200",
                            arabicName = "طقم جنزير ناقل حركة دي آي دي ياباني للنبض",
                            category = "سحب وجر",
                            price = 2950.00,
                            imageUrl = "https://images.unsplash.com/photo-1542282088-72c9c27ed0cd?auto=format",
                            brand = "Bajaj",
                            model = "Pulsar 200",
                            year = 2021,
                            description = "جنزير مطور بحلقات O-ring شديدة التحمل لمنع تسرب الشحوم وحماية العجلات الخلفية من الانزلاق لـ Pulsar 200.",
                            stock = 12
                        ),
                        ProductEntity(
                            name = "Bajaj Pulsar Rear Shock Absorber Pair",
                            arabicName = "طقم مساعدين خلفيين هيدروليك نبض",
                            category = "مساعدين ونظام تعليق",
                            price = 3800.00,
                            oldPrice = 4250.00,
                            imageUrl = "https://images.unsplash.com/photo-1449426468159-d96dbf08f19f?auto=format",
                            brand = "Bajaj",
                            model = "Pulsar 200",
                            year = 2021,
                            description = "مساعد خلفي غازي لتعديل امتصاص الصدمات في الطرق غير الممهدة، مناسب للمشاوير الطويلة والعمل الشاق.",
                            stock = 8,
                            isOffer = true
                        ),
                        ProductEntity(
                            name = "Vespa Primavera Carbon Performance Drive Belt",
                            arabicName = "سير نقل حركة كربون رياضي فيسبا",
                            category = "ناقل حركة",
                            price = 1950.00,
                            imageUrl = "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format",
                            brand = "Vespa",
                            model = "Primavera 150",
                            year = 2023,
                            description = "سير فتييس أصلي معزز بألياف الكربون للفيسبا بريمافيرا، عمر خدمة مضاعف ونقل مباشر للقوة لجميع السرعات.",
                            stock = 18
                        ),
                        ProductEntity(
                            name = "Keeway Superlight Air Filter Assembly",
                            arabicName = "فلتر هواء متكامل ميتاليك كي واي",
                            category = "فلاتر",
                            price = 450.00,
                            imageUrl = "https://images.unsplash.com/photo-1542282088-72c9c27ed0cd?auto=format",
                            brand = "Keeway",
                            model = "Superlight",
                            year = 2019,
                            description = "فلتر تنقية هواء عالي السعة لمنع تسرب ذرات الغبار وحماية الكاربراتير الخاص بمحركات كي واي سوبرلايت.",
                            stock = 30
                        ),
                        ProductEntity(
                            name = "Keeway Superlight Genuine Exhaust Wrap",
                            arabicName = "عازل حرارة شكمان كي واي الفاخر",
                            category = "إكسسوارات",
                            price = 890.00,
                            imageUrl = "https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format",
                            brand = "Keeway",
                            model = "Superlight",
                            year = 2019,
                            description = "طقم لفافات حماية وعزل الشكمان من فرط الحرارة بتصميم رياضي كلاسيكي يناسب دراجات كروزر سوبرلايت.",
                            stock = 5,
                            isOffer = true
                        )
                    )
                    dao.insertProducts(initialProducts)
                }
            }

            // Put a baseline post in community if empty
            communityPosts.first().let { current ->
                if (current.isEmpty()) {
                    val defaultPosts = listOf(
                        CommunityPostEntity(
                            author = "أمجد الدالي - رابطة CBR مصر",
                            avatar = "https://images.unsplash.com/photo-1449426468159-d96dbf08f19f?auto=format",
                            content = "شباب، تم تجربة تيل فرامل بريمبو الجديد على هوندا CBR 150 موديل 2022 المتوفر على تطبيق تروس، الفرامل فرقت 180 درجة والثبات عالي جداً! أرشحه بشدة.",
                            likesCount = 42,
                            commentsCount = 12
                        ),
                        CommunityPostEntity(
                            author = "الأسطورة صابر ميكانيكي شبرا",
                            avatar = "https://images.unsplash.com/photo-1542282088-72c9c27ed0cd?auto=format",
                            content = "لكل اللي بيسألوا عن بوجيهات الإيريديوم مع موتوسيكلات الراليات الكبيرة من هوندا أو ياماها، البوجيهات دي مش بس بتديك توفير بنزين، دي كمان بتمنع تكتكة الماكينة خالص الصبح في الساقعة. وفرناها جملة للميكانيكية هنا بسعر مميز.",
                            likesCount = 98,
                            commentsCount = 27
                        )
                    )
                    for (post in defaultPosts) {
                        dao.insertPost(post)
                    }
                }
            }
        }
    }

    // 6. Compatibility logic
    fun setFilterBrand(brand: String) {
        _selectedBrand.value = brand
        _selectedModel.value = "" // reset child when parent shifts
    }

    fun setFilterModel(model: String) {
        _selectedModel.value = model
    }

    fun setFilterYear(year: Int?) {
        _selectedYear.value = year
    }

    fun setQuery(q: String) {
        _searchQuery.value = q
    }

    fun clearFilters() {
        _selectedBrand.value = ""
        _selectedModel.value = ""
        _selectedYear.value = null
        _searchQuery.value = ""
    }

    // 7. Cart & Product Actions
    fun addToCart(productId: Int, quantity: Int = 1) {
        viewModelScope.launch {
            dao.insertCartItem(CartItemEntity(productId = productId, quantity = quantity))
        }
    }

    fun removeFromCart(productId: Int) {
        viewModelScope.launch {
            dao.deleteCartItem(productId)
        }
    }

    fun completeCheckout() {
        viewModelScope.launch {
            val currentCart = cartItems.value
            val currentProducts = allProducts.value
            if (currentCart.isEmpty()) return@launch

            var grandTotal = 0.0
            var itemCount = 0
            for (cartItem in currentCart) {
                val matched = currentProducts.find { it.id == cartItem.productId }
                if (matched != null) {
                    val priceToUse = if (_isB2BMode.value) matched.price * 0.85 else matched.price // 15% discount for B2B wholesale
                    grandTotal += priceToUse * cartItem.quantity
                    itemCount += cartItem.quantity
                }
            }

            // Create Order
            val newOrder = OrderEntity(
                orderDate = java.text.DateFormat.getDateTimeInstance().format(java.util.Date()),
                itemsCount = itemCount,
                totalPrice = grandTotal,
                status = "جاري مراجعة طلبك"
            )
            dao.insertOrder(newOrder)
            dao.clearCart() // empty cart after checkout
        }
    }

    // 8. B2B & Wholesale Controls
    fun toggleB2BMode() {
        _isB2BMode.value = !_isB2BMode.value
    }

    fun addWorkshopPoints(points: Int) {
        _workshopLoyaltyPoints.value += points
    }

    // 9. Admin Controls
    fun adminAddProduct(
        name: String,
        arabicName: String,
        category: String,
        price: Double,
        brand: String,
        model: String,
        year: Int,
        description: String,
        stock: Int
    ) {
        viewModelScope.launch {
            val newProduct = ProductEntity(
                name = name,
                arabicName = arabicName,
                category = category,
                price = price,
                imageUrl = "https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format",
                brand = brand,
                model = model,
                year = year,
                description = description,
                stock = stock
            )
            dao.insertProducts(listOf(newProduct))
        }
    }

    // 10. Garage Actions
    fun addBikeToGarage(brand: String, model: String, year: Int, odometer: Int, lastService: String, notes: String) {
        viewModelScope.launch {
            val bike = BikeProfileEntity(
                brand = brand,
                model = model,
                year = year,
                odometer = odometer,
                lastServiceDate = lastService,
                notes = notes
            )
            val bikeId = dao.insertBike(bike)
            // Add automated next maintenance reminders
            dao.insertMaintenanceEvent(
                MaintenanceEventEntity(
                    bikeId = bikeId.toInt(),
                    title = "تغيير زيت المحرك وفلتر الزيت القادم",
                    dueDate = "بعد 3,000 كم (أو خلال شهرين)"
                )
            )
            dao.insertMaintenanceEvent(
                MaintenanceEventEntity(
                    bikeId = bikeId.toInt(),
                    title = "فحص تيل الفرامل وضبط الجنزير",
                    dueDate = "بعد 5,000 كم"
                )
            )
        }
    }

    fun deleteBikeFromGarage(id: Int) {
        viewModelScope.launch {
            dao.deleteBike(id)
        }
    }

    // 11. AI Mechanic Chat Action (Gemini API Integration)
    fun askAiMechanic(userQuestion: String) {
        if (userQuestion.isBlank()) return
        
        // Add to history list immediately
        val historyList = _aiChatHistory.value.toMutableList()
        historyList.add(Pair(userQuestion, true))
        _aiChatHistory.value = historyList
        _isAiLoading.value = true

        viewModelScope.launch {
            val promptContext = """
                أنت هو 'ميكانيكي تروس الذكي'، خبير فني محترف في صيانة وإصلاح الموتوسيكلات بجميع أنواعها وموديلاتها (مثل هوندا، ياماها، باجاج، كي واي، تي في إس، هاري ديفيدسون والفيسبا الإيطالية) في السوق المصري والأفريقي.
                العملاء يسألونك عن مشاكل فنية، أعطال، تكتكات، رعشة، سخونة، أو رغبة في تحسين وتعديل الدراجات.
                يرجى الالتزام بالرد الدقيق باللغة العربية بأسلوب فني واثق، سهل الفهم، وعملي جداً، مع تجنب الكلام النظري العام. اقترح قطع غيار مطابقة محددة للمشكلة، وقدّم نصائح لسلامة السائق، ووجهه للورش المتخصصة في خريطة تروس عند اللزوم.
                السؤال الحالي من المستخدم: "$userQuestion"
            """.trimIndent()

            val response = geminiRepository.getGeminiResponse(promptContext)
            
            val updatedList = _aiChatHistory.value.toMutableList()
            updatedList.add(Pair(response, false))
            _aiChatHistory.value = updatedList
            _isAiLoading.value = false
        }
    }

    // 11b. Image/Audio Diagnostic scanning via Gemini
    fun diagnosticPhotoUpload(photoDescription: String, base64Image: String) {
        _isAiLoading.value = true
        _aiDiagnosticResult.value = null

        viewModelScope.launch {
            val visualPrompt = """
                تحتوي هذه الصورة على قطعة غيار موتوسيكل بها خلل أو مستخدم يحتاج للتعرف عليها.
                الوصف المرفق بالصورة: "$photoDescription".
                قم بتقديم تحليل فني فوري كخبير ميكانيكي لقطع غيار الدراجات النارية:
                1. التعرف على اسم القطعة الدقيق باللغة العربية والإنجليزية.
                2. الفحص البصري وتحديد مواطن الضرر التقريبي (كالتآكل، التشققات، أو نقص التشحيم).
                3. التوصية بالقطع البديلة المناسبة ونوعية المعادن/الخامات المفضلة كبديل، ومستويات الأسعار التقديرية بالجنيه المصري.
                4. الرمز التقريبي لمدى خطورة استمرار استخدامها (آمن مؤقتاً / يجب التغيير فوراً / تالف كلياً).
            """.trimIndent()

            val answer = geminiRepository.analyzePhoto(visualPrompt, base64Image)
            _aiDiagnosticResult.value = answer
            _isAiLoading.value = false
        }
    }

    fun clearDiagnosticState() {
        _aiDiagnosticResult.value = null
    }

    // 12. Community actions
    fun createCommunityPost(authorName: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val newPost = CommunityPostEntity(
                author = authorName,
                avatar = "https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format",
                content = content,
                likesCount = 0,
                commentsCount = 0
            )
            dao.insertPost(newPost)
        }
    }

    // 13. Value-Added Service requests
    fun triggerRoadsideRescue() {
        _activeRoadsideRescue.value = !_activeRoadsideRescue.value
    }

    fun triggerMobileMechanicRequest(locationDesc: String) {
        if (locationDesc.isBlank()) {
            _activeMobileMechanicRequest.value = null
        } else {
            _activeMobileMechanicRequest.value = "DISPATCHING" // will trigger dispatch animations in UI
        }
    }

    fun resetMobileMechanicState() {
        _activeMobileMechanicRequest.value = null
    }
}
