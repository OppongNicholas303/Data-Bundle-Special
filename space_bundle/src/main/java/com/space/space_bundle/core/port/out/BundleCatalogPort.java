package com.space.space_bundle.core.port.out;

import java.math.BigDecimal;

public interface BundleCatalogPort {

    boolean isValidBundle(String network, String bundleCode);

    BigDecimal getBundlePrice(String network, String bundleCode);
}
