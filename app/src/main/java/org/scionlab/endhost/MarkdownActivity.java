/*
 * Copyright (C) 2019-2020 Vera Clemens, Tom Kranz, Tom Heimbrodt, Elias Kuiter
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.scionlab.endhost;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import io.noties.markwon.Markwon;

public abstract class MarkdownActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markdown);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getMarkdownResId());
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        Markwon md = Markwon.create(this);
        TextView tv = findViewById(R.id.markdown);
        String markdown = Storage.from(this).readAssetFile(getString(getMarkdownResId()));
        md.setMarkdown(tv, markdown);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    abstract int getMarkdownResId();

    static class About extends MarkdownActivity {
        @Override
        int getMarkdownResId() {
            return R.string.about_asset_name;
        }
    }

    static class Help extends MarkdownActivity {
        @Override
        int getMarkdownResId() {
            return R.string.help_asset_name;
        }
    }
}
