# License

This project is dual-licensed, at your option, under either of:

* **Eclipse Public License, Version 2.0** (EPL-2.0) — see [`LICENSE_EPLv2.txt`](LICENSE_EPLv2.txt)
  ([online copy](https://www.eclipse.org/legal/epl-v20.html))
* **Eclipse Distribution License, Version 1.0** (EDL-1.0, a BSD-style license) — see [`LICENSE_EDLv1.txt`](LICENSE_EDLv1.txt)
  ([online copy](https://www.eclipse.org/org/documents/edl-v10.php))

This matches the licensing of the upstream [JTS Topology Suite](https://github.com/locationtech/jts),
of which this project is a Kotlin port.

## SPDX identifier

```
SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
```

(The Eclipse Distribution License v1.0 is a BSD-3-Clause license; `BSD-3-Clause`
is its registered SPDX identifier.)

## Note on `OSGEO_LICENSE.txt`

[`OSGEO_LICENSE.txt`](OSGEO_LICENSE.txt) contains the BSD-3-Clause grant carried
over from the upstream JTS Topology Suite (covering the original computational
geometry code this project is ported from). It is also the canonical BSD-3-Clause
text that GitHub's license detector recognizes, so the repository is correctly
identified as `EPL-2.0, BSD-3-Clause` — matching upstream JTS. GitHub does not
recognize `LICENSE_EDLv1.txt` on its own because its heading is not a registered
SPDX identifier, even though the EDL-1.0 is itself a BSD-3-Clause license.
