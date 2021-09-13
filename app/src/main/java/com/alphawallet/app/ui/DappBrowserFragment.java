package com.alphawallet.app.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.FragmentMessenger;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.SendTransactionInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.URLLoadInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletConnectActions;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.WalletConnectService;
import com.alphawallet.app.ui.QRScanning.QRScanner;
import com.alphawallet.app.ui.widget.OnDappClickListener;
import com.alphawallet.app.ui.widget.OnDappHomeNavClickListener;
import com.alphawallet.app.ui.widget.OnHistoryItemRemovedListener;
import com.alphawallet.app.ui.widget.adapter.DappBrowserSuggestionsAdapter;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.DappBrowserSwipeInterface;
import com.alphawallet.app.ui.widget.entity.DappBrowserSwipeLayout;
import com.alphawallet.app.ui.widget.entity.ItemClickListener;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.DappBrowserUtils;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.QRParser;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.DappBrowserViewModel;
import com.alphawallet.app.viewmodel.DappBrowserViewModelFactory;
import com.alphawallet.app.web3.OnEthCallListener;
import com.alphawallet.app.web3.OnSignMessageListener;
import com.alphawallet.app.web3.OnSignPersonalMessageListener;
import com.alphawallet.app.web3.OnSignTransactionListener;
import com.alphawallet.app.web3.OnSignTypedMessageListener;
import com.alphawallet.app.web3.OnWalletAddEthereumChainObjectListener;
import com.alphawallet.app.web3.Web3View;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject;
import com.alphawallet.app.web3.entity.Web3Call;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.ParseMagicLink;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmResults;

import static com.alphawallet.app.C.ETHER_DECIMALS;
import static com.alphawallet.app.C.RESET_TOOLBAR;
import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;
import static com.alphawallet.app.entity.Operation.SIGN_DATA;
import static com.alphawallet.app.entity.tokens.Token.TOKEN_BALANCE_PRECISION;
import static com.alphawallet.app.ui.MyAddressActivity.KEY_ADDRESS;
import static com.alphawallet.app.util.KeyboardUtils.showKeyboard;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class DappBrowserFragment extends BaseFragment implements OnSignTransactionListener, OnSignPersonalMessageListener,
        OnSignTypedMessageListener, OnSignMessageListener, OnEthCallListener, OnWalletAddEthereumChainObjectListener,
        URLLoadInterface, ItemClickListener, OnDappClickListener, OnDappHomeNavClickListener, OnHistoryItemRemovedListener, DappBrowserSwipeInterface, SignAuthenticationCallback,
        ActionSheetCallback
{
    private static final String TAG = DappBrowserFragment.class.getSimpleName();
    private static final String DAPP_BROWSER = "DAPP_BROWSER";
    private static final String MY_DAPPS = "MY_DAPPS";
    private static final String DISCOVER_DAPPS = "DISCOVER_DAPPS";
    private static final String HISTORY = "HISTORY";
    public static final String SEARCH = "SEARCH";
    public static final String PERSONAL_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";
    public static final String CURRENT_FRAGMENT = "currentFragment";
    private static final String CURRENT_URL = "urlInBar";
    private static final String WALLETCONNECT_CHAINID_ERROR = "Error: ChainId missing or not supported";
    private ValueCallback<Uri[]> uploadMessage;
    private WebChromeClient.FileChooserParams fileChooserParams;
    private RealmResults<RealmToken> realmUpdate;

    private ActionSheetDialog confirmationDialog;

    public static final int REQUEST_FILE_ACCESS = 31;
    public static final int REQUEST_FINE_LOCATION = 110;
    public static final int REQUEST_CAMERA_ACCESS = 111;

    /**
     Below object is used to set Animation duration for expand/collapse and rotate
     */
    private final int ANIMATION_DURATION = 100;

    @Inject
    DappBrowserViewModelFactory dappBrowserViewModelFactory;
    private DappBrowserViewModel viewModel;

    private DappBrowserSwipeLayout swipeRefreshLayout;
    private Web3View web3;
    private AutoCompleteTextView urlTv;
    private ProgressBar progressBar;
    private Wallet wallet;
    private NetworkInfo activeNetwork;
    private AWalletAlertDialog resultDialog;
    private DappBrowserSuggestionsAdapter adapter;
    private AlertDialog chainSwapDialog;
    private String loadOnInit;
    private boolean homePressed;
    private AddEthereumChainPrompt addCustomChainDialog;

    private final Fragment myDappsFragment;
    private final Fragment discoverDappsFragment;
    private final Fragment browserHistoryFragment;

    private Toolbar toolbar;
    private ImageView back;
    private ImageView next;
    private ImageView clear;
    private ImageView refresh;
    private TextView currentNetwork;
    private ImageView currentNetworkCircle;
    private LinearLayout currentNetworkClicker;
    private FrameLayout webFrame;
    private TextView balance;
    private TextView symbol;
    private View layoutNavigation;
    private GeolocationPermissions.Callback geoCallback = null;
    private PermissionRequest requestCallback = null;
    private String geoOrigin;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String walletConnectSession;

    private String currentWebpageTitle;
    private String currentFragment;

    private DAppFunction dAppFunction;

    @Nullable
    private Disposable disposable;

    public DappBrowserFragment()
    {
        myDappsFragment = new MyDappsFragment();
        discoverDappsFragment = new DiscoverDappsFragment();
        browserHistoryFragment = new BrowserHistoryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        LocaleUtils.setActiveLocale(getContext());
        super.onCreate(savedInstanceState);

        getChildFragmentManager().addFragmentOnAttachListener((fManager, f) -> {
            if (getContext() != null && f.getTag() != null)
            {
                switch (f.getTag())
                {
                    case DISCOVER_DAPPS:
                        ((DiscoverDappsFragment) f).setCallbacks(this);
                        break;
                    case MY_DAPPS:
                        ((MyDappsFragment) f).setCallbacks(this);
                        break;
                    case HISTORY:
                        ((BrowserHistoryFragment) f).setCallbacks(this, this);
                        break;
                    case DAPP_BROWSER:
                        break;
                    default:
                        //no init
                        break;
                }
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        homePressed = false;
        if (currentFragment == null) currentFragment = DAPP_BROWSER;
        attachFragment(currentFragment);
        if ((web3 == null || viewModel == null) && getActivity() != null) //trigger reload
        {
            ((HomeActivity)getActivity()).resetFragment(WalletPage.DAPP_BROWSER);
        }
        else
        {
            web3.setWebLoadCallback(this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        LocaleUtils.setActiveLocale(getContext());
        int webViewID = CustomViewSettings.minimiseBrowserURLBar() ? R.layout.fragment_webview_compact : R.layout.fragment_webview;
        View view = inflater.inflate(webViewID, container, false);
        initViewModel();
        initView(view);
        setupAddressBar();

        loadOnInit = null;

        // Load url from a link within the app
        if (getArguments() != null && getArguments().getString("url") != null) {
            String url = getArguments().getString("url");
            loadOnInit = url;
        } else {
            String lastUrl = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(CURRENT_URL, "");
            if (savedInstanceState != null)
            {
                lastUrl = savedInstanceState.getString(CURRENT_URL, "");
            }

            attachFragment(DAPP_BROWSER);
            loadOnInit = TextUtils.isEmpty(lastUrl) ? getDefaultDappUrl() : lastUrl;
        }

        return view;
    }

    private void attachFragment(Fragment fragment, String tag)
    {
        Fragment testFrag = getChildFragmentManager().findFragmentByTag(tag);
        if (testFrag != null && testFrag.isVisible() && !testFrag.isDetached())
        {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.frame, fragment)
                    .commitAllowingStateLoss();
        }
        else if (tag != null && getHost() != null && getChildFragmentManager().findFragmentByTag(tag) == null)
        {
            showFragment(fragment, tag);
        }
    }

    private void attachFragment(String tag) {
        if (tag != null && getHost() != null && getChildFragmentManager().findFragmentByTag(tag) == null)
        {
            Fragment f = null;
            switch (tag)
            {
                case DISCOVER_DAPPS:
                    f = discoverDappsFragment;
                    break;
                case MY_DAPPS:
                    f = myDappsFragment;
                    break;
                case HISTORY:
                    f = browserHistoryFragment;
                    break;
                case DAPP_BROWSER: //special case - dapp browser is no fragments loaded
                    addToBackStack(DAPP_BROWSER);
                    break;
            }

            if (f != null && !f.isAdded()) showFragment(f, tag);
        }
    }

    private void showFragment(Fragment fragment, String tag) {
        addToBackStack(tag);
        getChildFragmentManager().beginTransaction()
                .add(R.id.frame, fragment, tag)
                .commit();

        setBackForwardButtons();
    }

    private void detachFragments()
    {
        detachFragment(MY_DAPPS);
        detachFragment(DISCOVER_DAPPS);
        detachFragment(HISTORY);
        detachFragment(SEARCH);
    }

    public void homePressed()
    {
        homePressed = true;
        detachFragments();
        currentFragment = DAPP_BROWSER;
        if (urlTv != null)
            urlTv.getText().clear();
        if (web3 != null)
        {
            resetDappBrowser();
        }

        //blank forward / backward arrows
        setBackForwardButtons();
    }

    @Override
    public void onDappHomeNavClick(int position) {
        detachFragments();
        switch (position) {
            case 0: {
                addToBackStack(MY_DAPPS);
                attachFragment(myDappsFragment, MY_DAPPS);
                break;
            }
            case 1: {
                addToBackStack(DISCOVER_DAPPS);
                attachFragment(discoverDappsFragment, DISCOVER_DAPPS);
                break;
            }
            case 2: {
                addToBackStack(HISTORY);
                attachFragment(browserHistoryFragment, HISTORY);
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void onDappClick(@NotNull DApp dapp) {
        addToBackStack(DAPP_BROWSER);
        loadUrl(dapp.getUrl());
    }

    @Override
    public void onHistoryItemRemoved(DApp dApp) {
        adapter.removeSuggestion(dApp);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        viewModel.onDestroy();
        if (realmUpdate != null) realmUpdate.removeAllChangeListeners();
        if (disposable != null && !disposable.isDisposed()) disposable.dispose();
    }

    private void setupMenu(@NotNull View baseView)
    {
        refresh = baseView.findViewById(R.id.refresh);
        final MenuItem reload = toolbar.getMenu().findItem(R.id.action_reload);
        final MenuItem share = toolbar.getMenu().findItem(R.id.action_share);
        final MenuItem scan = toolbar.getMenu().findItem(R.id.action_scan);
        final MenuItem add = toolbar.getMenu().findItem(R.id.action_add_to_my_dapps);
        final MenuItem history = toolbar.getMenu().findItem(R.id.action_history);
        final MenuItem bookmarks = toolbar.getMenu().findItem(R.id.action_my_dapps);
        final MenuItem clearCache = toolbar.getMenu().findItem(R.id.action_clear_cache);

        if (reload != null) reload.setOnMenuItemClickListener(menuItem -> {
            reloadPage();
            return true;
        });
        if (share != null) share.setOnMenuItemClickListener(menuItem -> {
            if (web3.getUrl() != null && currentFragment != null && currentFragment.equals(DAPP_BROWSER)) {
                if (getContext() != null) viewModel.share(getContext(), web3.getUrl());
            }
            else
            {
                displayNothingToShare();
            }
            return true;
        });
        if (scan != null) scan.setOnMenuItemClickListener(menuItem -> {
            viewModel.startScan(getActivity());
            return true;
        });
        if (add != null) add.setOnMenuItemClickListener(menuItem -> {
            viewModel.addToMyDapps(getContext(), currentWebpageTitle, urlTv.getText().toString());
            return true;
        });
        if (history != null) history.setOnMenuItemClickListener(menuItem -> {
            addToBackStack(HISTORY);
            attachFragment(browserHistoryFragment, HISTORY);
            return true;
        });
        if (bookmarks != null) bookmarks.setOnMenuItemClickListener(menuItem -> {
            addToBackStack(MY_DAPPS);
            attachFragment(myDappsFragment, MY_DAPPS);
            return true;
        });
        if (clearCache != null) clearCache.setOnMenuItemClickListener(menuItem -> {
            viewModel.onClearBrowserCacheClicked(getContext());
            return true;
        });
    }

    private void initView(@NotNull View view) {
        web3 = view.findViewById(R.id.web3view);
        progressBar = view.findViewById(R.id.progressBar);
        urlTv = view.findViewById(R.id.url_tv);
        webFrame = view.findViewById(R.id.frame);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setRefreshInterface(this);

        toolbar = view.findViewById(R.id.address_bar);
        layoutNavigation = view.findViewById(R.id.layout_navigator);

        //If you are wondering about the strange way the menus are inflated - this is required to ensure
        //that the menu text gets created with the correct localisation under every circumstance
        MenuInflater inflater = new MenuInflater(LocaleUtils.getActiveLocaleContext(getContext()));
        if (CustomViewSettings.minimiseBrowserURLBar())
        {
            inflater.inflate(R.menu.menu_scan, toolbar.getMenu());
        }
        else if (getDefaultDappUrl() != null)
        {
            inflater.inflate(R.menu.menu_bookmarks, toolbar.getMenu());
        }
        refresh = view.findViewById(R.id.refresh);
        setupMenu(view);

        RelativeLayout layout = view.findViewById(R.id.address_bar_layout);
        layout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        refresh.setOnClickListener(v -> reloadPage());

        back = view.findViewById(R.id.back);
        back.setOnClickListener(v -> backPressed());

        next = view.findViewById(R.id.next);
        next.setOnClickListener(v -> goToNextPage());

        clear = view.findViewById(R.id.clear_url);
        clear.setOnClickListener(v -> {
            clearAddressBar();
        });

        currentNetworkClicker = view.findViewById(R.id.network_holder);
        currentNetworkClicker.setOnClickListener(v -> openNetworkSelection());
        currentNetwork = view.findViewById(R.id.network_text);
        currentNetworkCircle = view.findViewById(R.id.network_colour);
        balance = view.findViewById(R.id.balance);
        symbol = view.findViewById(R.id.symbol);
        web3.setWebLoadCallback(this);

        webFrame.setOnApplyWindowInsetsListener(resizeListener);
    }

    private void displayNothingToShare() {
        if (getActivity() == null) return;
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setTitle(getString(R.string.nothing_to_share));
        resultDialog.setMessage(getString(R.string.nothing_to_share_message));
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.setCancelable(true);
        resultDialog.show();
    }

    private void openNetworkSelection() {
        Intent intent = new Intent(getContext(), SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, true);
        if (activeNetwork != null) intent.putExtra(C.EXTRA_CHAIN_ID, activeNetwork.chainId);
        getNetwork.launch(intent);
    }

    private void clearAddressBar() {
        if (urlTv.getText().toString().isEmpty()) {
            cancelSearchSession();
        } else {
            urlTv.getText().clear();
            openURLInputView();
            KeyboardUtils.showKeyboard(urlTv); //ensure keyboard shows here so we can listen for it being cancelled
        }
    }

    private void setupAddressBar() {
        adapter = new DappBrowserSuggestionsAdapter(
                getContext(),
                viewModel.getDappsMasterList(getContext()),
                this::onItemClick
        );
        urlTv.setAdapter(null);

        urlTv.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_GO)
            {
                String urlText = urlTv.getText().toString();
                handled = loadUrl(urlText);
                detachFragments();
                cancelSearchSession();
            }
            return handled;
        });

        // Both these are required, the onFocus listener is required to respond to the first click.
        urlTv.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && getActivity() != null) openURLInputView();
        });

        urlTv.setOnClickListener(v -> {
            openURLInputView();
        });

        urlTv.setShowSoftInputOnFocus(true);

        urlTv.setOnLongClickListener(v -> {
            urlTv.dismissDropDown();
            return false;
        });

        urlTv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                adapter.setHighlighted(editable.toString());
            }
        });
    }

    @Override
    public void comeIntoFocus()
    {
        if (viewModel != null) { viewModel.checkForNetworkChanges(); }
    }

    @Override
    public void leaveFocus()
    {
        if (web3 != null) web3.clearFocus();
        if (urlTv != null) urlTv.clearFocus();
    }

    // TODO: Move all nav stuff to widget
    private void openURLInputView() {
        urlTv.setAdapter(null);
        expandCollapseView(currentNetwork, false);
        expandCollapseView(layoutNavigation, false);

        disposable = Observable.zip(
                Observable.interval(600, TimeUnit.MILLISECONDS).take(1),
                Observable.fromArray(clear), (interval, item) -> item)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::postBeginSearchSession);
    }

    private void postBeginSearchSession(@NotNull ImageView item)
    {
        urlTv.setAdapter(adapter);
        urlTv.showDropDown();
        if (item.getVisibility() == View.GONE)
        {
            expandCollapseView(item, true);
            showKeyboard(urlTv);
        }
    }

    /**
     * Used to expand or collapse the view
     */
    private synchronized void expandCollapseView(@NotNull View view, boolean expandView)
    {
        //detect if view is expanded or collapsed
        boolean isViewExpanded = view.getVisibility() == View.VISIBLE;

        //Collapse view
        if (isViewExpanded && !expandView)
        {
            int finalWidth = view.getWidth();
            ValueAnimator valueAnimator = slideAnimator(finalWidth, 0, view);
            valueAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            valueAnimator.start();
        }
        //Expand view
        else if (!isViewExpanded && expandView)
        {
            view.setVisibility(View.VISIBLE);

            int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            view.measure(widthSpec, heightSpec);
            int width = view.getMeasuredWidth();
            ValueAnimator valueAnimator = slideAnimator(0, width, view);
            valueAnimator.start();
        }
    }

    @NotNull
    private ValueAnimator slideAnimator(int start, int end, final View view) {

        final ValueAnimator animator = ValueAnimator.ofInt(start, end);

        animator.addUpdateListener(valueAnimator -> {
            // Update Height
            int value = (Integer) valueAnimator.getAnimatedValue();

            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = value;
            view.setLayoutParams(layoutParams);
        });
        animator.setDuration(ANIMATION_DURATION);
        return animator;
    }

    private void addToBackStack(String nextFragment)
    {
        currentFragment = nextFragment;
    }

    private void addToForwardStack(String prevFragment)
    {
        currentFragment = prevFragment;
    }

    private void cancelSearchSession() {
        detachFragment(SEARCH);
        KeyboardUtils.hideKeyboard(urlTv);
        setBackForwardButtons();
    }

    private void shrinkSearchBar()
    {
        if (toolbar != null)
        {
            toolbar.getMenu().setGroupVisible(R.id.dapp_browser_menu, true);
            expandCollapseView(currentNetwork, true);
            expandCollapseView(layoutNavigation, true);
            clear.setVisibility(View.GONE);
            urlTv.dismissDropDown();
        }
    }

    private void detachFragment(String tag) {
        if (!isAdded()) return; //the dappBrowserFragment itself may not yet be attached.
        Fragment fragment = getChildFragmentManager().findFragmentByTag(tag);
        if (fragment != null && fragment.isVisible() && !fragment.isDetached()) {
            getChildFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss();
        }
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this, dappBrowserViewModelFactory)
                .get(DappBrowserViewModel.class);
        viewModel.activeNetwork().observe(getViewLifecycleOwner(), this::onNetworkChanged);
        viewModel.defaultWallet().observe(getViewLifecycleOwner(), this::onDefaultWallet);
        activeNetwork = viewModel.getActiveNetwork();
    }

    private void startBalanceListener()
    {
        if (wallet == null || activeNetwork == null) return;

        if (realmUpdate != null) realmUpdate.removeAllChangeListeners();
        realmUpdate = viewModel.getRealmInstance(wallet).where(RealmToken.class)
                .equalTo("address", TokensRealmSource.databaseKey(activeNetwork.chainId, "eth"))
                .equalTo("chainId", activeNetwork.chainId).findAllAsync();
        realmUpdate.addChangeListener(realmTokens -> {
            //update balance
            if (realmTokens.size() == 0) return;
            RealmToken realmToken = realmTokens.first();
            balance.setVisibility(View.VISIBLE);
            symbol.setVisibility(View.VISIBLE);
            String newBalanceStr = BalanceUtils.getScaledValueFixed(new BigDecimal(realmToken.getBalance()), ETHER_DECIMALS, TOKEN_BALANCE_PRECISION);
            balance.setText(newBalanceStr);
            symbol.setText(activeNetwork.getShortName());
        });
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet = wallet;
        setupWeb3();
        startBalanceListener();
    }

    /**
     * Called by openDapp(Dapp url) to automatically switch to the required network when required
     * @param chainId
     */
    public void switchNetwork(int chainId)
    {
        if (activeNetwork != null && activeNetwork.chainId == chainId) return; //not required
        viewModel.setNetwork(chainId);

        //setup network selection and init web3 with updated chain
        activeNetwork = viewModel.getNetworkInfo(chainId);
        currentNetwork.setText(activeNetwork.getShortName());
        Utils.setChainColour(currentNetworkCircle, activeNetwork.chainId);
        viewModel.findWallet();
    }

    private void onNetworkChanged(NetworkInfo networkInfo)
    {
        boolean networkChanged = networkInfo != null && (activeNetwork == null || activeNetwork.chainId != networkInfo.chainId);
        this.activeNetwork = networkInfo;
        if (networkInfo != null)
        {
            currentNetwork.setText(networkInfo.getShortName());
            Utils.setChainColour(currentNetworkCircle, networkInfo.chainId);
            viewModel.findWallet();

            if (networkChanged && isOnHomePage())
                resetDappBrowser(); //trigger a reset if on homepage
        }
        else
        {
            openNetworkSelection();
            resetDappBrowser();
        }
    }

    ActivityResultLauncher<Intent> getNewNetwork = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                int networkId = result.getData().getIntExtra(C.EXTRA_CHAIN_ID, 1);
                loadNewNetwork(networkId);
                reloadPage();
            });

    private void launchNetworkPicker()
    {
        Intent intent = new Intent(getContext(), SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, true);
        if (activeNetwork != null) intent.putExtra(C.EXTRA_CHAIN_ID, activeNetwork.chainId);
        getNewNetwork.launch(intent);
    }

    private void launchWalletConnectSessionCancel()
    {
        String sessionId = walletConnectSession != null ? viewModel.getSessionId(walletConnectSession) : "";
        Intent bIntent = new Intent(getContext(), WalletConnectService.class);
        bIntent.setAction(String.valueOf(WalletConnectActions.CLOSE.ordinal()));
        bIntent.putExtra("session", sessionId);
        getContext().startService(bIntent);
        reloadPage();
    }

    private void displayCloseWC()
    {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
        resultDialog = new AWalletAlertDialog(getContext());
        resultDialog.setIcon(WARNING);
        resultDialog.setTitle(R.string.title_wallet_connect);
        resultDialog.setMessage(getString(R.string.unsupported_walletconnect));
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            launchWalletConnectSessionCancel();
            launchNetworkPicker();
            resultDialog.dismiss();
        });
        resultDialog.show();
    }

    private void setupWeb3() {
        web3.setChainId(activeNetwork.chainId);
        web3.setRpcUrl(viewModel.getNetworkNodeRPC(activeNetwork.chainId));
        web3.setWalletAddress(new Address(wallet.address));

        web3.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView webview, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    refresh.setEnabled(true);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                    swipeRefreshLayout.setRefreshing(true);
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage msg)
            {
                boolean ret = super.onConsoleMessage(msg);

                if (msg.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
                {
                    if (msg.message().contains(WALLETCONNECT_CHAINID_ERROR))
                    {
                        displayCloseWC();
                    }
                }

                return ret;
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                currentWebpageTitle = title;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                requestCameraPermission(request);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback)
            {
                super.onGeolocationPermissionsShowPrompt(origin, callback);
                requestGeoPermission(origin, callback);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fCParams)
            {
                if (filePathCallback == null) return true;
                uploadMessage = filePathCallback;
                fileChooserParams = fCParams;
                if (checkReadPermission()) return requestUpload();
                else return true;
            }
        });

        web3.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String[] prefixCheck = url.split(":");
                if (prefixCheck.length > 1)
                {
                    Intent intent;
                    switch (prefixCheck[0])
                    {
                        case C.DAPP_PREFIX_TELEPHONE:
                            intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse(url));
                            startActivity(Intent.createChooser(intent, "Call " + prefixCheck[1]));
                            return true;
                        case C.DAPP_PREFIX_MAILTO:
                            intent = new Intent(Intent.ACTION_SENDTO);
                            intent.setData(Uri.parse(url));
                            startActivity(Intent.createChooser(intent, "Email: " + prefixCheck[1]));
                            return true;
                        case C.DAPP_PREFIX_ALPHAWALLET:
                            if(prefixCheck[1].equals(C.DAPP_SUFFIX_RECEIVE)) {
                                viewModel.showMyAddress(getContext());
                                return true;
                            }
                            break;
                        case C.DAPP_PREFIX_WALLETCONNECT:
                            //start walletconnect
                            if (wallet.type == WalletType.WATCH)
                            {
                                showWalletWatch();
                            }
                            else
                            {
                                walletConnectSession = url;
                                if (getContext() != null)
                                    viewModel.handleWalletConnect(getContext(), url, activeNetwork);
                            }
                            return true;
                        default:
                            break;
                    }
                }

                setUrlText(url);
                return false;
            }
        });

        web3.setOnSignMessageListener(this);
        web3.setOnSignPersonalMessageListener(this);
        web3.setOnSignTransactionListener(this);
        web3.setOnSignTypedMessageListener(this);
        web3.setOnEthCallListener(this);
        web3.setOnWalletAddEthereumChainObjectListener(this);

        if (loadOnInit != null)
        {
            addToBackStack(DAPP_BROWSER);
            web3.loadUrl(Utils.formatUrl(loadOnInit), getWeb3Headers());
            setUrlText(Utils.formatUrl(loadOnInit));
            loadOnInit = null;
        }
    }

    private void setUrlText(String newUrl)
    {
        if (getView() == null) return;
        urlTv = getView().findViewById(R.id.url_tv);
        urlTv.setText(newUrl);
        setBackForwardButtons();
    }

    ActivityResultLauncher<String> getContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) uploadMessage.onReceiveValue(new Uri[] { uri });
                }
            });

    ActivityResultLauncher<Intent> getNetwork = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                int networkId = result.getData().getIntExtra(C.EXTRA_CHAIN_ID, 1);
                loadNewNetwork(networkId);
            });

    private void loadNewNetwork(int newNetworkId)
    {
        if (activeNetwork == null || activeNetwork.chainId != newNetworkId)
        {
            balance.setVisibility(View.GONE);
            symbol.setVisibility(View.GONE);
            viewModel.setNetwork(newNetworkId);
        }
        //refresh URL page
        reloadPage();
    }

    protected boolean requestUpload()
    {
        try
        {
            getContent.launch(determineMimeType(fileChooserParams));
        }
        catch (ActivityNotFoundException e)
        {
            uploadMessage = null;
            Toast.makeText(getActivity().getApplicationContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    public void setCurrentGasIndex(int gasSelectionIndex, BigDecimal customGasPrice, BigDecimal customGasLimit, long expectedTxTime, long customNonce)
    {
        if (confirmationDialog != null && confirmationDialog.isShowing())
        {
            confirmationDialog.setCurrentGasIndex(gasSelectionIndex, customGasPrice, customGasLimit, expectedTxTime, customNonce);
        }
    }

    @Override
    public void onSignMessage(final EthereumMessage message) {
        handleSignMessage(message);
    }

    @Override
    public void onSignPersonalMessage(final EthereumMessage message) {
        handleSignMessage(message);
    }

    @Override
    public void onSignTypedMessage(@NotNull EthereumTypedMessage message)
    {
        if (message.getPrehash() == null || message.getMessageType() == SignMessageType.SIGN_ERROR)
        {
            web3.onSignCancel(message.getCallbackId());
        }
        else
        {
            handleSignMessage(message);
        }
    }

    @Override
    public void onEthCall(Web3Call call)
    {
        Single.fromCallable(() -> {
            //let's make the call
            Web3j web3j = TokenRepository.getWeb3jService(activeNetwork.chainId);
            //construct call
            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(wallet.address, call.to.toString(), call.payload);
            return web3j.ethCall(transaction, call.blockParam).send();
        }).map(EthCall::getValue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> web3.onCallFunctionSuccessful(call.leafPosition, result),
                        error -> web3.onCallFunctionError(call.leafPosition, error.getMessage()))
                .isDisposed();
    }

    @Override
    public void OnWalletAddEthereumChainObject(WalletAddEthereumChainObject chainObj)
    {
        // read chain value
        int chainId = chainObj.getChainId();
        final NetworkInfo info = viewModel.getNetworkInfo(chainId);

        // handle unknown network
        if (info == null) {
            // show add custom chain dialog
            addCustomChainDialog = new AddEthereumChainPrompt(getContext(), chainObj, chainObject -> {
                viewModel.addCustomChain(chainObject);
                loadNewNetwork(chainObj.getChainId());
                addCustomChainDialog.dismiss();
            });
            addCustomChainDialog.show();
            return;
        }

        //Don't show dialog if network doesn't need to be changed or if alredy showing
        if (activeNetwork.chainId == chainId || (chainSwapDialog != null && chainSwapDialog.isShowing())) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.request_change_chain, info.name, String.valueOf(info.chainId)))
                .setPositiveButton(R.string.dialog_ok, (d, w) -> loadNewNetwork(info.chainId))
                .setNegativeButton(R.string.action_cancel, (d, w) -> chainSwapDialog.dismiss())
                .setCancelable(true);

        //Warn if we're switching between network types
        if (EthereumNetworkRepository.hasRealValue(activeNetwork.chainId) != EthereumNetworkRepository.hasRealValue(info.chainId))
        {
            if (EthereumNetworkRepository.hasRealValue(info.chainId))
            {
                builder.setTitle(R.string.warning_switch_to_main);
            }
            else
            {
                builder.setTitle(R.string.warning_switching_to_test);
            }
        }

        chainSwapDialog = builder.create();
        chainSwapDialog.show();
    }

    private void handleSignMessage(Signable message)
    {
        dAppFunction = new DAppFunction() {
            @Override
            public void DAppError(Throwable error, Signable message) {
                web3.onSignCancel(message.getCallbackId());
                confirmationDialog.dismiss();
            }

            @Override
            public void DAppReturn(byte[] data, Signable message) {
                String signHex = Numeric.toHexString(data);
                Log.d(TAG, "Initial Msg: " + message.getMessage());
                web3.onSignMessageSuccessful(message, signHex);

                confirmationDialog.success();
            }
        };

        if (confirmationDialog == null || !confirmationDialog.isShowing())
        {
            confirmationDialog = new ActionSheetDialog(getActivity(), this, this, message);
            confirmationDialog.setCanceledOnTouchOutside(false);
            confirmationDialog.show();
            confirmationDialog.fullExpand();
        }
    }

    @Override
    public void onSignTransaction(Web3Transaction transaction, String url)
    {
        try
        {
            viewModel.updateGasPrice(activeNetwork.chainId); //start updating gas price right before we open
            //TODO: Ensure we have received gas price before continuing
            //minimum for transaction to be valid: recipient and value or payload
            if ((confirmationDialog == null || !confirmationDialog.isShowing()) &&
                    (transaction.recipient.equals(Address.EMPTY) && transaction.payload != null) // Constructor
                    || (!transaction.recipient.equals(Address.EMPTY) && (transaction.payload != null || transaction.value != null))) // Raw or Function TX
            {
                Token token = viewModel.getTokenService().getTokenOrBase(activeNetwork.chainId, transaction.recipient.toString());
                confirmationDialog = new ActionSheetDialog(getActivity(), transaction, token,
                        "", transaction.recipient.toString(), viewModel.getTokenService(), this);
                confirmationDialog.setURL(url);
                confirmationDialog.setCanceledOnTouchOutside(false);
                confirmationDialog.show();
                confirmationDialog.fullExpand();

                viewModel.calculateGasEstimate(wallet, Numeric.hexStringToByteArray(transaction.payload),
                        activeNetwork.chainId, transaction.recipient.toString(), new BigDecimal(transaction.value))
                        .map(limit -> convertToGasLimit(limit, transaction.gasLimit))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(estimate -> confirmationDialog.setGasEstimate(estimate),
                                Throwable::printStackTrace)
                        .isDisposed();

                return;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        onInvalidTransaction(transaction);
        web3.onSignCancel(transaction.leafPosition);
    }

    /**
     * Debug function for assisting testing
     *
     * @param tx
     * @return
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    private Web3Transaction getDebugTx(@NotNull Web3Transaction tx)
    {
        return new Web3Transaction(
                tx.recipient,
                tx.contract,
                tx.value,
                BigInteger.ZERO,//tx.gasPrice,
                tx.gasLimit,
                tx.nonce,
                tx.payload,
                tx.leafPosition
        );
    }

    private BigInteger convertToGasLimit(EthEstimateGas estimate, BigInteger txGasLimit)
    {
        try
        {
            if (!estimate.hasError() && estimate.getAmountUsed().compareTo(BigInteger.ZERO) > 0)
            {
                return estimate.getAmountUsed();
            }
            else
            {
                return txGasLimit;
            }
        }
        catch (Exception e)
        {
            //
        }

        return txGasLimit;
    }

    //Transaction failed to be sent
    private void txError(Throwable throwable)
    {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
        resultDialog = new AWalletAlertDialog(getContext());
        resultDialog.setIcon(ERROR);
        resultDialog.setTitle(R.string.error_transaction_failed);
        resultDialog.setMessage(throwable.getMessage());
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.show();

        if (confirmationDialog != null && confirmationDialog.isShowing()) confirmationDialog.dismiss();
    }

    private void showWalletWatch()
    {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
        resultDialog = new AWalletAlertDialog(getContext());
        resultDialog.setIcon(AWalletAlertDialog.WARNING);
        resultDialog.setTitle(R.string.title_wallet_connect);
        resultDialog.setMessage(R.string.action_watch_account);
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.show();
    }

    private void onInvalidTransaction(Web3Transaction transaction)
    {
        if (getActivity() == null) return;
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setIcon(AWalletAlertDialog.ERROR);
        resultDialog.setTitle(getString(R.string.invalid_transaction));

        if (transaction.recipient.equals(Address.EMPTY) && (transaction.payload == null || transaction.value != null))
        {
            resultDialog.setMessage(getString(R.string.contains_no_recipient));
        }
        else if (transaction.payload == null && transaction.value == null)
        {
            resultDialog.setMessage(getString(R.string.contains_no_value));
        }
        else
        {
            resultDialog.setMessage(getString(R.string.contains_no_data));
        }
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.setCancelable(true);
        resultDialog.show();
    }

    public void backPressed()
    {
        if (web3 == null || back == null || back.getAlpha() == 0.3f) return;
        if (web3.canGoBack())
        {
            checkBackClickArrowVisibility(); //to make arrows function correctly - don't want to wait for web page to load to check back/forwards - this looks clunky
            loadSessionUrl(-1);
            web3.goBack();
            detachFragments();
        }
        else if (!web3.getUrl().equalsIgnoreCase(getDefaultDappUrl()))
        {
            //load homepage
            homePressed = true;
            web3.loadUrl(getDefaultDappUrl(), getWeb3Headers());
            setUrlText(getDefaultDappUrl());
            checkBackClickArrowVisibility();
        }
        else
        {
            checkBackClickArrowVisibility();
        }
    }

    private void goToNextPage()
    {
        if (next.getAlpha() == 0.3f) return;
        if (web3.canGoForward())
        {
            checkForwardClickArrowVisibility();
            loadSessionUrl(1);
            web3.goForward();
        }
    }

    /**
     * Check if this is the last web item and the last fragment item.
     */
    private void checkBackClickArrowVisibility()
    {
        //will this be last item?
        WebBackForwardList sessionHistory = web3.copyBackForwardList();
        int nextIndex = sessionHistory.getCurrentIndex() - 1;

        String nextUrl;

        if (nextIndex >= 0)
        {
            WebHistoryItem newItem = sessionHistory.getItemAtIndex(nextIndex);
            nextUrl = newItem.getUrl();
        }
        else
        {
            nextUrl = getDefaultDappUrl();
        }

        if (nextUrl.equalsIgnoreCase(getDefaultDappUrl()))
        {
            back.setAlpha(0.3f);
        }
        else
        {
            back.setAlpha(1.0f);
        }
    }

    /**
     * After a forward click while web browser active, check if forward and back arrows should be updated.
     * Note that the web item only becomes history after the next page is loaded, so if the next item is new, then
     */
    private void checkForwardClickArrowVisibility()
    {
        WebBackForwardList sessionHistory = web3.copyBackForwardList();
        int nextIndex = sessionHistory.getCurrentIndex() + 1;
        if (nextIndex >= sessionHistory.getSize() - 1) next.setAlpha(0.3f);
        else next.setAlpha(1.0f);
    }

    /**
     * Browse to relative entry with sanity check on value
     * @param relative relative addition or subtraction of browsing index
     */
    private void loadSessionUrl(int relative)
    {
        WebBackForwardList sessionHistory = web3.copyBackForwardList();
        int newIndex = sessionHistory.getCurrentIndex() + relative;
        if (newIndex < sessionHistory.getSize())
        {
            WebHistoryItem newItem = sessionHistory.getItemAtIndex(newIndex);
            if (newItem != null)
            {
                setUrlText(newItem.getUrl());
            }
        }
    }

    @Override
    public void onWebpageLoaded(String url, String title)
    {
        if (getContext() == null) return; //could be a late return from dead fragment
        if (homePressed)
        {
            homePressed = false;
            if (currentFragment.equals(DAPP_BROWSER) && url.equals(getDefaultDappUrl()))
            {
                web3.clearHistory();
            }
        }
        DApp dapp = new DApp(title, url);
        DappBrowserUtils.addToHistory(getContext(), dapp);
        adapter.addSuggestion(dapp);
        onWebpageLoadComplete();

        if (urlTv != null) urlTv.setText(url);
    }

    @Override
    public void onWebpageLoadComplete()
    {
        handler.post(this::setBackForwardButtons); //execute on UI thread
    }

    private void setBackForwardButtons()
    {
        WebBackForwardList sessionHistory;
        boolean canBrowseBack = false;
        boolean canBrowseForward = false;

        if (web3 != null)
        {
            sessionHistory = web3.copyBackForwardList();
            canBrowseBack = web3.canGoBack() || !isOnHomePage();
            canBrowseForward = (sessionHistory != null && sessionHistory.getCurrentIndex() < sessionHistory.getSize() - 1);
        }

        if (back != null)
        {
            if (canBrowseBack)
            {
                back.setAlpha(1.0f);
            }
            else
            {
                back.setAlpha(0.3f);
            }
        }

        if (next != null)
        {
            if (canBrowseForward)
            {
                next.setAlpha(1.0f);
            }
            else
            {
                next.setAlpha(0.3f);
            }
        }
    }

    private boolean isOnHomePage()
    {
        if (web3 != null)
        {
            String url = web3.getUrl();
            return EthereumNetworkRepository.isDefaultDapp(url);
        }
        else
        {
            return false;
        }
    }

    private boolean loadUrl(String urlText)
    {
        detachFragments();
        addToBackStack(DAPP_BROWSER);
        cancelSearchSession();
        if (checkForMagicLink(urlText)) return true;
        web3.loadUrl(Utils.formatUrl(urlText), getWeb3Headers());
        setUrlText(Utils.formatUrl(urlText));
        web3.requestFocus();
        viewModel.setLastUrl(getContext(), urlText);
        Activity current = getActivity();
        if (current != null)
        {
            current.sendBroadcast(new Intent(RESET_TOOLBAR));
        }
        return true;
    }

    public void loadDirect(String urlText)
    {
        if (web3 == null)
        {
            if (getActivity() != null) ((HomeActivity)getActivity()).resetFragment(WalletPage.DAPP_BROWSER);
        }
        else
        {
            // reset initial url, to avoid issues with initial load
            loadOnInit = null;

            cancelSearchSession();
            addToBackStack(DAPP_BROWSER);
            setUrlText(Utils.formatUrl(urlText));
            web3.loadUrl(Utils.formatUrl(urlText), getWeb3Headers());
            //ensure focus isn't on the keyboard
            KeyboardUtils.hideKeyboard(urlTv);
            web3.requestFocus();
        }
    }

    /* Required for CORS requests */
    @NotNull
    @Contract(" -> new")
    private Map<String, String> getWeb3Headers()
    {
        //headers
        return new HashMap<String, String>() {{
            put("Connection", "close");
            put("Content-Type", "text/plain");
            put("Access-Control-Allow-Origin", "*");
            put("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
            put("Access-Control-Max-Age", "600");
            put("Access-Control-Allow-Credentials", "true");
            put("Access-Control-Allow-Headers", "accept, authorization, Content-Type");
        }};
    }

    public void reloadPage() {
        if (currentFragment.equals(DAPP_BROWSER))
        {
            refresh.setEnabled(false);
            web3.reload();
        }
    }

    @Override
    public void onItemClick(String url)
    {
        addToBackStack(DAPP_BROWSER);
        loadUrl(url);
    }

    public void testRecoverAddressFromSignature(@NotNull String message, String sig)
    {
        String prefix = PERSONAL_MESSAGE_PREFIX + message.length();
        byte[] msgHash = (prefix + message).getBytes();

        byte[] signatureBytes = Numeric.hexStringToByteArray(sig);
        Sign.SignatureData sd = sigFromByteArray(signatureBytes);
        String addressRecovered;

        try
        {
            BigInteger recoveredKey = Sign.signedMessageToKey(msgHash, sd);
            addressRecovered = "0x" + Keys.getAddress(recoveredKey);
            if (BuildConfig.DEBUG) System.out.println("Recovered: " + addressRecovered);
        }
        catch (SignatureException e)
        {
            e.printStackTrace();
        }
    }

    private void resetDappBrowser()
    {
        web3.clearHistory();
        web3.stopLoading();
        web3.loadUrl(getDefaultDappUrl(), getWeb3Headers());
        setUrlText(getDefaultDappUrl());
    }

    public void handleQRCode(int resultCode, Intent data, FragmentMessenger messenger)
    {
        //result
        String qrCode = null;
        try
        {
            switch (resultCode)
            {
                case Activity.RESULT_OK:
                    if (data != null)
                    {
                        qrCode = data.getStringExtra(C.EXTRA_QR_CODE);
                        if (qrCode == null || checkForMagicLink(qrCode)) return;
                        QRParser parser = QRParser.getInstance(EthereumNetworkRepository.extraChains());
                        QRResult result = parser.parse(qrCode);
                        switch (result.type)
                        {
                            case ADDRESS:
                                //ethereum address was scanned. In dapp browser what do we do? maybe populate an input field with address?
                                copyToClipboard(result.getAddress());
                                break;
                            case PAYMENT:
                                //EIP681 payment request scanned, should go to send
                                viewModel.showSend(getContext(), result);
                                break;
                            case TRANSFER:
                                //EIP681 transfer, go to send
                                viewModel.showSend(getContext(), result);
                                break;
                            case FUNCTION_CALL:
                                //EIP681 function call. TODO: create function call confirmation. For now treat same way as tokenscript function call
                                break;
                            case URL:
                                loadUrlRemote(qrCode);
                                break;
                            case OTHER:
                                qrCode = null;
                                break;
                        }
                    }
                    break;
                case QRScanner.DENY_PERMISSION:
                    showCameraDenied();
                    break;
                case QRScanner.WALLET_CONNECT:
                    return;
                default:
                    break;
            }
        }
        catch (Exception e)
        {
            qrCode = null;
        }

        if (qrCode == null && getActivity() != null)
        {
            Toast.makeText(getActivity(), R.string.toast_invalid_code, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads URL from remote process; this converts a request to load URL which isn't on the app's thread
     * @param qrCode
     */
    private void loadUrlRemote(final String qrCode)
    {
        handler.post(() -> loadUrl(qrCode));
    }

    private void showCameraDenied()
    {
        if (getActivity() == null) return;
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setTitle(R.string.title_dialog_error);
        resultDialog.setMessage(R.string.error_camera_permission_denied);
        resultDialog.setIcon(ERROR);
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.show();
    }

    private void copyToClipboard(String address)
    {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(KEY_ADDRESS, address);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(getActivity(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    private boolean checkForMagicLink(String data)
    {
        try
        {
            ParseMagicLink parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());
            if (parser.parseUniversalLink(data).chainId > 0) //see if it's a valid link
            {
                //handle magic link import
                viewModel.showImportLink(getActivity(), data);
                return true;
            }
        }
        catch (SalesOrderMalformed e)
        {
            //
        }

        return false;
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    private boolean checkReadPermission()
    {
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        else
        {
            String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
            getActivity().requestPermissions(permissions, REQUEST_FILE_ACCESS);
            return false;
        }
    }

    // Handles the requesting of the fine location permission.
    // Note: If you intend allowing geo-location in your app you need to ask the permission.
    private void requestGeoPermission(String origin, GeolocationPermissions.Callback callback)
    {
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            geoCallback = callback;
            geoOrigin = origin;
            String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            getActivity().requestPermissions(permissions, REQUEST_FINE_LOCATION);
        }
        else
        {
            callback.invoke(origin, true, false);
        }
    }

    // Handles the requesting of the camera permission.
    private void requestCameraPermission(@NotNull PermissionRequest request)
    {
        final String[] requestedResources = request.getResources();
        requestCallback = request;
        for (String r : requestedResources)
        {
            if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
            {
                final String[] permissions = new String[]{Manifest.permission.CAMERA};
                getActivity().requestPermissions(permissions, REQUEST_CAMERA_ACCESS);
            }
        }
    }

    public void gotCameraAccess(@NotNull String[] permissions, int[] grantResults)
    {
        boolean cameraAccess = false;
        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i].equals(Manifest.permission.CAMERA) && grantResults[i] != -1)
            {
                cameraAccess = true;
                if (requestCallback != null) requestCallback.grant(requestCallback.getResources()); //now we can grant permission
            }
        }
        if (!cameraAccess) Toast.makeText(getContext(), "Permission not given", Toast.LENGTH_SHORT).show();
    }

    public void gotGeoAccess(@NotNull String[] permissions, int[] grantResults)
    {
        boolean geoAccess = false;
        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] != -1) geoAccess = true;
        }
        if (!geoAccess) Toast.makeText(getContext(), "Permission not given", Toast.LENGTH_SHORT).show();
        if (geoCallback != null && geoOrigin != null) geoCallback.invoke(geoOrigin, geoAccess, false);
    }

    public void gotFileAccess(@NotNull String[] permissions, int[] grantResults)
    {
        boolean fileAccess = false;
        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[i] != -1) fileAccess = true;
        }

        if (fileAccess) requestUpload();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_FRAGMENT, currentFragment);
        outState.putString(CURRENT_URL, urlTv.getText().toString());
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putString(CURRENT_FRAGMENT, currentFragment)
                .putString(CURRENT_URL, urlTv.getText().toString())
                .apply();
    }

    @Override
    public void RefreshEvent()
    {
        //determine scroll position
        Log.i("Touch", "SCROLL: " + web3.getScrollY());
        if (web3.getScrollY() == 0)
        {
            loadUrl(web3.getUrl());
        }
    }

    @Override
    public int getCurrentScrollPosition()
    {
        return web3.getScrollY();
    }

    // this is called when the signing is approved by the user (e.g. fingerprint / PIN)
    @Override
    public void gotAuthorisation(boolean gotAuth)
    {
        if (confirmationDialog != null && confirmationDialog.isShowing())
        {
            confirmationDialog.dismiss();
        }
    }

    @Override
    public void gotAuthorisationForSigning(boolean gotAuth, Signable messageToSign)
    {
        if (gotAuth)
        {
            viewModel.completeAuthentication(SIGN_DATA);
            viewModel.signMessage(messageToSign, dAppFunction);
        }
        else
        {
            web3.onSignCancel(messageToSign.getCallbackId());
        }
    }

    /**
     * Endpoint from PIN/Swipe authorisation
     * @param gotAuth
     */
    public void pinAuthorisation(boolean gotAuth)
    {
        if (confirmationDialog != null && confirmationDialog.isShowing())
        {
            confirmationDialog.completeSignRequest(gotAuth);
        }
    }

    @Override
    public void cancelAuthentication()
    {

    }

    /**
     * ActionSheet interfaces
     */

    @Override
    public void getAuthorisation(SignAuthenticationCallback callback)
    {
        viewModel.getAuthorisation(wallet, getActivity(), callback);
    }

    @Override
    public void sendTransaction(Web3Transaction finalTx)
    {
        final SendTransactionInterface callback = new SendTransactionInterface()
        {
            @Override
            public void transactionSuccess(Web3Transaction web3Tx, String hashData)
            {
                confirmationDialog.transactionWritten(hashData);
                web3.onSignTransactionSuccessful(web3Tx, hashData);
            }

            @Override
            public void transactionError(long callbackId, Throwable error)
            {
                confirmationDialog.dismiss();
                txError(error);
                web3.onSignCancel(callbackId);
            }
        };

        viewModel.sendTransaction(finalTx, activeNetwork.chainId, callback);
    }

    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {
        //actionsheet dismissed - if action not completed then user cancelled
        if (!actionCompleted)
        {
            //actionsheet dismissed before completing signing.
            web3.onSignCancel(callbackId);
        }
    }

    @Override
    public void notifyConfirm(String mode)
    {
        if (getActivity() != null) ((HomeActivity)getActivity()).useActionSheet(mode);
    }

    public void selected()
    {
        //start gas update cycle when user selects Dapp browser
        if (viewModel != null && activeNetwork != null)
        {
            viewModel.updateGasPrice(activeNetwork.chainId);
        }
    }

    // Handle resizing the browser view when the soft keyboard pops up and goes.
    // The issue this fixes is where you need to enter data at the bottom of the webpage,
    // and the keyboard hides the input field
    // Need to handle the inverse event where the keyboard is hidden, and we size the page back
    // (Remembering to allow for the navigation bar).
    private final View.OnApplyWindowInsetsListener resizeListener = (v, insets) -> {
        if (v == null || getActivity() == null) { return insets; }

        Rect r = new Rect();
        v.getWindowVisibleDisplayFrame(r);

        int heightDifference = v.getRootView().getHeight() - (r.bottom - r.top);
        int navBarHeight = ((HomeActivity) getActivity()).getNavBarHeight();

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) webFrame.getLayoutParams();

        // check if we need to resize the webview. If we don't do this, the keyboard covers the bottom of the site
        // and might be obscuring elements the user needs to see while typing
        if (heightDifference > 0 && webFrame != null && layoutParams.bottomMargin != heightDifference)
        {
            //go into 'shrink' mode so no web site data is hidden
            layoutParams.bottomMargin = heightDifference;
            webFrame.setLayoutParams(layoutParams);
        }
        else if (heightDifference == 0 && layoutParams.bottomMargin != navBarHeight)
        {
            //go back into full screen mode, and expand URL bar out
            layoutParams.bottomMargin = navBarHeight;
            webFrame.setLayoutParams(layoutParams);
            shrinkSearchBar();
        }

        return insets;
    };

    // Required for if we have status bar showing
    public void softKeyboardVisible()
    {
        if (getActivity() == null) { return; }

        Rect r = new Rect();
        webFrame.getWindowVisibleDisplayFrame(r);

        int heightDifference = webFrame.getRootView().getHeight() - r.bottom;

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) webFrame.getLayoutParams();

        layoutParams.bottomMargin = heightDifference;
        webFrame.setLayoutParams(layoutParams);
    }

    public void softKeyboardGone()
    {
        if (getActivity() == null) { return; }
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) webFrame.getLayoutParams();
        layoutParams.bottomMargin = ((HomeActivity) getActivity()).getNavBarHeight();
        webFrame.setLayoutParams(layoutParams);
        shrinkSearchBar();
    }

    @NotNull
    private String determineMimeType(@NotNull WebChromeClient.FileChooserParams fileChooserParams)
    {
        if (fileChooserParams == null || fileChooserParams.getAcceptTypes().length == 0) return "*/*"; // Allow anything
        String mime;
        String firstType = fileChooserParams.getAcceptTypes()[0];

        if (fileChooserParams.getAcceptTypes().length == 1)
        {
            mime = firstType;
        }
        else
        {
            //TODO: Resolve types
            switch (firstType)
            {
                case "png":
                case "gif":
                case "svg":
                case "jpg":
                case "jpeg":
                case "image/*":
                    mime = "image/*";
                    break;

                case "mp4":
                case "x-msvideo":
                case "x-ms-wmv":
                case "mpeg4-generic":
                case "video/*":
                    mime = "video/*";
                    break;

                case "mpeg":
                case "aac":
                case "wav":
                case "ogg":
                case "midi":
                case "x-ms-wma":
                case "audio/*":
                    mime = "audio/*";
                    break;

                case "pdf":
                    mime = "application/*";
                    break;

                case "xml":
                case "csv":
                    mime = "text/*";
                    break;

                default:
                    mime = "*/*";
            }
        }

        return mime;
    }

    private String getDefaultDappUrl()
    {
        return EthereumNetworkRepository.defaultDapp(activeNetwork != null ? activeNetwork.chainId : 0);
    }
}
